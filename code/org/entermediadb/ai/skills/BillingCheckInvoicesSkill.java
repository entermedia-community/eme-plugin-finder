package org.entermediadb.ai.skills;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.llm.BasicLlmResponse;
import org.entermediadb.asset.MediaArchive;
import org.entermedia.stripe.StripePaymentProcessor;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.data.Searcher;
import org.openedit.users.User;

/**
 * Java port of plugins/catalog/html/events/scripts/billing/checkinvoices.groovy.
 *
 * Generates recurring invoices for active products whose next bill date falls within the
 * next 15 days, then attempts Stripe autopay on every invoice left in "invoiced" status
 * with isautopaid=true. The notification emails are sent by BillingSendInvoiceSkill (the Java
 * port of sendinvoice.groovy), which runs as the next step of the billing scenario - this skill
 * no longer fires the billing/sendinvoices media event, so invoices are not emailed twice.
 */
public class BillingCheckInvoicesSkill extends BaseSkill
{
	private static final Log log = LogFactory.getLog(BillingCheckInvoicesSkill.class);

	@Override
	public void process(AgentContext inContext)
	{
		log.info("Checking Invoices");
		int created = generateRecurringInvoices(); //status=active
		int[] autopay = payAutoPaidInvoices();

		BasicLlmResponse response = new BasicLlmResponse();
		response.setMessage("Billing check complete: " + created + " recurring invoice(s) generated, "
				+ autopay[0] + " of " + autopay[1] + " pending invoice(s) auto-paid. Invoice notifications are sent by the Billing Send Invoice step.");
		inContext.setLastResponse(response);
		super.process(inContext);
	}

	private int[] payAutoPaidInvoices()
	{
		MediaArchive mediaArchive = getMediaArchive();
		Searcher invoiceSearcher = mediaArchive.getSearcher("collectiveinvoice");

		Calendar today = Calendar.getInstance();
		Collection invoices = invoiceSearcher.query().exact("paymentstatus", "invoiced").exact("isautopaid", "true").search();
		int paid = 0;
		if (invoices.size() > 0)
		{
			log.info("Auto-Paid pending invoices " + invoices.size() + " found");
			for (Iterator invoiceIterator = invoices.iterator(); invoiceIterator.hasNext();)
			{
				Data invoice = invoiceSearcher.loadData((Data) invoiceIterator.next());
				String collectionid = (String) invoice.getValue("collectionid");
				String email = "billing+" + collectionid + "@entermediadb.com";
				Map<String, Object> customer = null;
				try
				{
					customer = getStripeProcessor(collectionid).getCustomer(email);
				}
				catch (Exception e)
				{
					log.error("Error getting Stripe customer for " + email, e);
				}
				if (customer != null)
				{
					Map<String, Object> sourcesData = (Map<String, Object>) customer.get("sources");
					ArrayList<Map<String, Object>> sources = sourcesData != null ? (ArrayList<Map<String, Object>>) sourcesData.get("data") : null;
					if (sources != null && sources.size() > 0)
					{
						Map<String, Object> source = sources.get(0);
						Searcher payments = mediaArchive.getSearcher("transaction");
						Data payment = payments.createNewData();
						payment.setValue("paymenttype", "stripe");
						payment.setValue("totalprice", invoice.getValue("totalprice"));
						boolean chargeSuccess = false;
						try
						{
							chargeSuccess = getStripeProcessor(collectionid).createCharge(payment, (String) customer.get("id"), (String) source.get("id"), invoice, email);
						}
						catch (Exception e)
						{
							log.error("Error creating Stripe charge for invoice " + invoice.getId(), e);
						}

						if (chargeSuccess)
						{
							invoice.setValue("paymentstatus", "paid");
							invoice.setValue("invoicepaidon", today.getTime());
							paid++;
						}
						else
						{
							invoice.setValue("paymentstatus", "autopayfailed");
							invoice.setValue("paymentstatusreason", "CreditCard failed");
						}
					}
					else
					{
						invoice.setValue("paymentstatus", "autopayfailed");
						invoice.setValue("paymentstatusreason", "No Credit Cards Stored");
					}
				}
				else
				{
					invoice.setValue("paymentstatus", "autopayfailed");
					invoice.setValue("paymentstatusreason", "No Customer");
				}
				invoiceSearcher.saveData(invoice);
			}
		}
		return new int[] { paid, invoices.size() };
	}

	private StripePaymentProcessor getStripeProcessor(String inCollectionId)
	{
		StripePaymentProcessor processor = new StripePaymentProcessor();
		processor.setCollectionId(inCollectionId);
		processor.setMediaArchive(getMediaArchive());
		return processor;
	}

	private int generateRecurringInvoices()
	{
		MediaArchive mediaArchive = getMediaArchive();
		Searcher productSearcher = mediaArchive.getSearcher("collectiveproduct");

		int daysToExpire = 15; // invoice will expire in (days)
		Calendar today = Calendar.getInstance();

		//from today and 15 up
		Calendar since = Calendar.getInstance();
		since.add(Calendar.DAY_OF_YEAR, +daysToExpire);

		Collection pendingProducts = productSearcher.query().exact("recurring", "true").exact("billingstatus", "active").before("nextbillon", since.getTime()).sort(
			"nextbillonUp").search();
		log.info(pendingProducts);
		int created = 0;
		if (!pendingProducts.isEmpty())
		{
			log.info("Found " + pendingProducts.size() + " products");
			for (Iterator productIterator = pendingProducts.iterator(); productIterator.hasNext();)
			{
				MultiValued product = (MultiValued) productSearcher.loadData((Data) productIterator.next());
				Date nextBillOn = product.getDate("nextbillon"); // July 01
				Date lastbilleddate = product.getDate("lastgeneratedinvoicedate"); // Aug 01    //Save for what bill date it goes with
				if (lastbilleddate != null)
				{

					Calendar within15ofduedate = Calendar.getInstance();
					within15ofduedate.setTime(nextBillOn);

					within15ofduedate.add(Calendar.DAY_OF_YEAR, -15);

					if (lastbilleddate.after(within15ofduedate.getTime()))
					{
						log.info("Already recently sent invoice for " + product.getName() + " one on: " + lastbilleddate + " next bill on:" + nextBillOn);
						continue; // skip if end date is before today
					}
				}
				log.info("Creating recurring invoices for: " + product.getId() + " " + product + " Period: " + product.getValue("recurringperiod"));

				Searcher invoiceSearcher = mediaArchive.getSearcher("collectiveinvoice");
				Data invoice = invoiceSearcher.createNewData();

				HashMap<String, Object> productItem = new HashMap<String, Object>();
				productItem.put("productid", product.getValue("id"));
				productItem.put("productname", product.getName());
				String productname = product.getName();
				productItem.put("productquantity", 1);
				productItem.put("productprice", product.getValue("productprice"));
				Collection items = new ArrayList();
				items.add(productItem);
				invoice.setValue("productlist", items);
				invoice.setValue("currencytype", product.getValue("currencytype"));
				invoice.setValue("paymentstatus", "sendinvoice");
				invoice.setValue("isautopaid", product.getValue("isautopaid"));
				invoice.setValue("collectionid", product.getValue("collectionid"));
				invoice.setValue("owner", product.getValue("owner"));
				invoice.setValue("totalprice", product.getValue("productprice"));
				invoice.setValue("isrecurring", "true");
				invoice.setValue("recurringperiod", product.getValue("recurringperiod"));
				invoice.setValue("duedate", nextBillOn);
				invoice.setValue("invoicedescription", product.getValue("productdescription"));
				invoice.setValue("notificationsent", "false");
				invoice.setValue("createdon", today.getTime());

				//name -subject
				String collectionid = (String) product.getValue("collectionid");
				Data collection = mediaArchive.getCachedData("librarycollection", collectionid);
				if (productname != null)
				{
					invoice.setValue("name", productname);
				}
				else if (collection != null)
				{
					String name = "${project} - ${invoicemonth} Invoice";
					invoice.setValue("name", name);
				}

				String contactsstring = "";
				if (product.getValue("sentto") != null)
				{
					contactsstring = (String) product.getValue("sentto");
				}
				else
				{
					Collection contacts = mediaArchive.query("librarycollectionusers").exact("collectionid", collectionid).exact("ontheteam", true).exact(
						"isbillingcontact", "true").search();
					if (contacts != null)
					{
						for (Iterator contactIterator = contacts.iterator(); contactIterator.hasNext();)
						{
							Data c = (Data) contactIterator.next();
							User user = mediaArchive.getUser((String) c.getValue("followeruser"));

							if (user != null && user.getEmail() != null)
							{
								if (contactsstring != "")
								{
									contactsstring = contactsstring + ", " + user.getEmail();
								}
								else
								{
									contactsstring = user.getEmail();
								}
							}
						}
					}
				}
				invoice.setValue("sentto", contactsstring);
				invoiceSearcher.saveData(invoice);
				created++;

				product.setValue("lastgeneratedinvoicedate", nextBillOn);

				log.info("Invoice Created for recurring product for: " + collection);

				Integer recurringperiod = product.getInt("recurringperiod");

				Calendar endbilldate = Calendar.getInstance();
				endbilldate.setTime(nextBillOn);
				endbilldate.add(Calendar.MONTH, recurringperiod);
				//invoice.setValue("enddate", endbilldate.getTime()); //Not used anymore?
				product.setValue("nextbillon", endbilldate.getTime());

				productSearcher.saveData(product);
				log.info("Next Invoice for: " + collection + ", will be generated: " + endbilldate.getTime());
			}
		}
		return created;
	}

}
