package org.entermediadb.ai.skills;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.llm.BasicLlmResponse;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.email.TemplateWebEmail;
import org.entermedia.invoice.InvoiceManager;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.data.Searcher;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.LocaleManager;
import org.openedit.util.URLUtilities;

/**
 * Java port of plugins/catalog/html/events/scripts/billing/sendinvoice.groovy.
 *
 * Sends invoice notification emails. With the optional "invoiceid" context value it handles a single
 * invoice: error status is left alone, unpaid invoices get their notification (paymentstatus becomes
 * "invoiced", notificationsent "true") and paid invoices that have not been notified yet get the paid
 * notification (notificationpaidsent "true"). Without an invoiceid it does a bulk pass over every
 * invoice with notificationsent=false and paymentstatus="sendinvoice", notifying the billing contacts
 * and the project team for each one.
 *
 * Deviations from the groovy script (all bug fixes, behavior otherwise identical):
 * - invoiceContactIterate returns after the missing-librarycollection branch; the original fell
 *   through into an NPE on librarycol.get("communitytag") which aborted the whole batch.
 * - The footer fallback checks invoiceemailfooter instead of invoiceemailheader (original bug).
 * - The notificationpaidsent / notifyprojectadmins branches never assigned invoiceemailfooter in the
 *   original, which NPE'd at the footer check; here those emails are sent without a footer.
 * - The bulk loop catches per-invoice errors so one failing invoice does not abort the rest of the
 *   batch (the failed invoice keeps its status and is retried on the next run).
 * - siteid: the groovy resolved it via context.findValue("siteid"), which is null in this deployment
 *   (producing a broken "null/..." template path). Here the catalog setting
 *   events_billing_notify_invoice_appid optionally overrides the app prefix, falling back to "site".
 * - The community tag is optional: the original aborted the email when the project had no
 *   communitytag. Here the email still goes out - the template falls back to the profile emails
 *   folder and the pay/action URL uses the siteroot catalog setting (or "/&lt;siteid&gt;") instead of
 *   the community externaldomain; communitylink/communityhome fall back the same way.
 */
public class BillingSendInvoiceSkill extends BaseSkill
{
	private static final Log log = LogFactory.getLog(BillingSendInvoiceSkill.class);

	@Override
	public void process(AgentContext inContext)
	{
		MediaArchive mediaArchive = getMediaArchive();
		Searcher invoiceSearcher = mediaArchive.getSearcher("collectiveinvoice");
		String locale = inContext.getLocale();

		Object invoiceidValue = inContext.getContextValue("invoiceid");
		String invoiceid = invoiceidValue != null ? String.valueOf(invoiceidValue) : null;

		String result;
		if (invoiceid != null)
		{
			log.info("Individual Invoice: " + invoiceid);
			Data invoice = getInvoiceManager().getInvoiceById(invoiceid);
			if (invoice == null)
			{
				result = "Invoice not found: " + invoiceid;
			}
			else if ("error".equals(invoice.getValue("paymentstatus")))
			{
				// notify about error?
				result = "Invoice " + invoiceid + " is in error status - no notification sent.";
			}
			else if (!"paid".equals(invoice.getValue("paymentstatus")))
			{
				invoiceContactIterate(mediaArchive, invoiceSearcher, invoice, "notificationsent", locale);
				invoice.setValue("paymentstatus", "invoiced");
				invoice.setValue("notificationsent", "true");
				invoiceSearcher.saveData(invoice);
				result = "Invoice " + invoiceid + " notification sent - paymentstatus set to invoiced.";
			}
			else if (!Boolean.valueOf((String) invoice.getValue("notificationpaidsent")))
			{
				log.info("Sending Paid Notification for " + invoiceid);
				invoiceContactIterate(mediaArchive, invoiceSearcher, invoice, "notificationpaidsent", locale);
				invoice.setValue("notificationpaidsent", "true");
				invoiceSearcher.saveData(invoice);
				result = "Paid notification sent for invoice " + invoiceid + ".";
			}
			else
			{
				result = "Invoice " + invoiceid + " is paid and the paid notification was already sent.";
			}
		}
		else
		{
			log.info("Sending Pending & Recurring Invoices...");
			int processed = sendInvoiceNotifications(mediaArchive, invoiceSearcher, locale);
			result = "Sent invoice notifications for " + processed + " pending invoice(s).";
		}

		BasicLlmResponse response = new BasicLlmResponse();
		response.setMessage(result);
		inContext.setLastResponse(response);
		super.process(inContext);
	}

	private int sendInvoiceNotifications(MediaArchive mediaArchive, Searcher invoiceSearcher, String locale)
	{
		Collection pendingNotificationInvoices = invoiceSearcher.query().exact("notificationsent", "false").exact("paymentstatus", "sendinvoice").search();

		int processed = 0;
		if (pendingNotificationInvoices.size() > 0)
		{
			log.info("Sending Notification for " + pendingNotificationInvoices.size() + " invoices");
			for (Iterator invoiceIterator = pendingNotificationInvoices.iterator(); invoiceIterator.hasNext();)
			{
				Data invoice = invoiceSearcher.loadData((Data) invoiceIterator.next());
				try
				{
					invoiceContactIterate(mediaArchive, invoiceSearcher, invoice, "notificationsent", locale);

					// Notify project Admins about invoice
					invoiceNotifyProject(mediaArchive, invoiceSearcher, invoice, locale);
					processed++;
				}
				catch (Exception e)
				{
					// the original let one failing invoice abort the whole batch; keep processing the rest
					log.error("Error sending notification for invoice " + invoice.getId(), e);
				}
			}
		}
		return processed;
	}

	private void invoiceContactIterate(MediaArchive mediaArchive, Searcher invoiceSearcher, Data invoice, String iteratorType, String locale)
	{
		String collectionid = (String) invoice.getValue("collectionid");
		Data librarycol = mediaArchive.getCachedData("librarycollection", collectionid);
		if (librarycol == null)
		{
			invoice.setValue(iteratorType, "true");
			invoiceSearcher.saveData(invoice);
			return; // the original fell through here into an NPE that aborted the batch
		}

		invoice.setValue("paymentstatusreason", null);

		String emails = (String) invoice.getValue("sentto");
		if (emails == null)
		{
			log.info("Error no email address to send invoice. Invoice: " + invoice.getId() + " " + invoice.getName());
			invoice.setValue("paymentstatus", "error");
			invoice.setValue("paymentstatusreason", "Empty Send To");
			invoiceSearcher.saveData(invoice);
			return;
		}
		List<String> emaillist = Arrays.asList(emails.split(","));

		boolean sent = false;
		if (emaillist.size() > 0)
		{
			for (String email : emaillist)
			{
				if (email != null && email.length() > 0)
				{
					sendinvoiceEmail(mediaArchive, email, invoice, librarycol, iteratorType, locale);
					sent = true; // needs better error handling
				}
			}
			if (sent)
			{
				// send Copy to bookkeeper@entermediadb.org
				String ccinvoices = mediaArchive.getCatalogSettingValue("invoice_cc_email");
				if (ccinvoices != null)
				{
					sendinvoiceEmail(mediaArchive, ccinvoices, invoice, librarycol, iteratorType, locale);
				}

				Calendar today = Calendar.getInstance();
				invoice.setValue("sentto", emails);
				invoice.setValue("sentdate", today.getTime());
				invoice.setValue(iteratorType, "true");

				if (!"paid".equals(invoice.getValue("paymentstatus"))) // TODO: Seems weird. Should this be if invoice.get("paymentstatus") == null
				{
					// mark as invoiced
					invoice.setValue("paymentstatus", "invoiced");
				}
			}
			else
			{
				invoice.setValue("paymentstatus", "error");
				invoice.setValue("paymentstatusreason", "Send email error");
				log.info("Error sending invoice to addresses: " + emails);
			}
			invoiceSearcher.saveData(invoice);
		}
		else
		{
			log.info("No email addresses to send Invoice for: " + librarycol + " (" + collectionid + ")");
		}
	}

	private void invoiceNotifyProject(MediaArchive mediaArchive, Searcher invoiceSearcher, Data invoice, String locale)
	{
		String collectionid = (String) invoice.getValue("collectionid");
		Data librarycol = mediaArchive.getCachedData("librarycollection", collectionid);
		if (librarycol == null)
		{
			log.info("No project team to notify for invoice " + invoice.getId() + " - missing librarycollection " + collectionid);
			return; // the original NPE'd here
		}

		String emails = (String) librarycol.getValue("contactemail");

		if (emails == null)
		{
			log.info("Error sending invoice to email to Project Team, missing contactemail. Invoice: " + invoice.getId() + " Client: " + invoice.getName()
				+ " invoiceNotifyProject");
			invoice.setValue("paymentstatus", "error");
			invoice.setValue("paymentstatusreason", "No contact email set on project");
			invoiceSearcher.saveData(invoice);
			return;
		}

		List<String> emaillist = Arrays.asList(emails.split(","));

		StringBuffer errorEmails = new StringBuffer();
		if (emaillist.size() > 0)
		{
			log.info("Sending email to  " + emaillist.size() + " members of: " + librarycol + " (" + collectionid + ")");
			for (String email : emaillist)
			{
				if (email != null && email.length() > 0)
				{
					try
					{
						sendinvoiceEmail(mediaArchive, email, invoice, librarycol, "notifyprojectadmins", locale);
					}
					catch (Throwable ex)
					{
						errorEmails.append(email + " " + ex.getMessage());
					}
				}
			}
			if (errorEmails.length() > 0)
			{
				invoice.setValue("paymentstatus", "error");
				invoice.setValue("paymentstatusreason", "Error sending email to: " + errorEmails);
				log.info("Error sending invoice to addresses: " + errorEmails);
			}
			invoiceSearcher.saveData(invoice);
		}
		else
		{
			log.info("No email addresses to send Notification for: " + librarycol + " (" + collectionid + ")");
		}
	}

	private void sendinvoiceEmail(MediaArchive mediaArchive, String contact, Data invoice, Data librarycol, String messagetype, String locale)
	{
		String siteid = getSiteId(mediaArchive);

		Data community = mediaArchive.getData("communitytag", (String) librarycol.getValue("communitytag"));
		if (community == null)
		{
			log.info("No Community Tag for Project: " + librarycol + " - sending email without community");
		}

		String communitypath = community != null ? (String) community.getValue("templatepath") : null;
		String template;
		if (communitypath != null)
		{
			template = siteid + communitypath + "/theme/emails/";
		}
		else
		{
			template = siteid + "/profile/theme/emails/";
		}

		String actionUrl;
		if (community != null)
		{
			actionUrl = (String) community.getValue("externaldomain") + "/" + librarycol.getValue("urlname");
		}
		else
		{
			String siteRoot = getSiteRoot(mediaArchive);
			actionUrl = (siteRoot != null ? siteRoot : "/" + siteid) + "/" + librarycol.getValue("urlname");
		}

		Map<String, Object> objects = new HashMap<String, Object>();

		String subject;
		String invoiceemailheader;
		String invoiceemailfooter = null;

		String collectionid = librarycol.getId();

		String invoicenumber = (String) invoice.getValue("invoicenumber");

		String project = (String) librarycol.getName();

		String month = "";

		if ("notificationsent".equals(messagetype))
		{
			actionUrl = actionUrl + "/services/paynow.html?invoiceid=" + invoice.getValue("id") + "&collectionid=" + collectionid;
			actionUrl = actionUrl + "&contactemail=" + contact;

			subject = "[${project}] Invoice #${invoicenumber}";

			if (Boolean.valueOf((String) invoice.getValue("isrecurring")))
			{
				if (invoice.getName() != null)
				{
					subject = "[${project}] " + invoice.getName();
				}
				else
				{
					subject = "[${project}] ${invoicemonth} Invoice";
				}
			}

			Date duedate = ((MultiValued) invoice).getDate("duedate");
			if (duedate != null)
			{
				month = getLocaleManager().getMonthName(duedate, locale);
			}

			if (((MultiValued) invoice).getInt("recurringperiod") == 1)
			{
				template = template + "invoice-to-client-monthly.html";
			}
			else
			{
				template = template + "invoice-to-client.html";
			}

			// Invoice Template from collection
			invoiceemailheader = (String) librarycol.getValue("invoiceemailheader");
			if (invoiceemailheader == null || invoiceemailheader.equals(""))
			{
				invoiceemailheader = mediaArchive.getCatalogSettingValue("invoice_email_header");
			}

			invoiceemailfooter = (String) librarycol.getValue("invoiceemailfooter");
			if (invoiceemailfooter == null || invoiceemailfooter.equals("")) // the original checked invoiceemailheader here
			{
				invoiceemailfooter = mediaArchive.getCatalogSettingValue("invoice_email_footer");
			}
		}
		else if ("notificationoverduesent".equals(messagetype))
		{
			subject = "Overdue Invoice for " + librarycol.getName();
			template = template + "send-overdue-invoice-event.html";
			// Invoice Template from collection
			invoiceemailheader = (String) librarycol.getValue("invoiceemailheader");
			if (invoiceemailheader == null || invoiceemailheader.equals(""))
			{
				invoiceemailheader = mediaArchive.getCatalogSettingValue("invoice_email_header");
			}

			invoiceemailfooter = (String) librarycol.getValue("invoiceemailfooter");
			if (invoiceemailfooter == null || invoiceemailfooter.equals("")) // the original checked invoiceemailheader here
			{
				invoiceemailfooter = mediaArchive.getCatalogSettingValue("invoice_email_footer");
			}
		}
		else if ("notificationpaidsent".equals(messagetype))
		{
			subject = "Payment Received " + librarycol.getName();
			template = template + "send-paid-invoice-event.html";

			invoiceemailheader = (String) librarycol.getValue("invoicepaidemail");
			if (invoiceemailheader == null || invoiceemailheader.equals(""))
			{
				invoiceemailheader = mediaArchive.getCatalogSettingValue("invoice_paid_email");
			}
			// the original never assigned invoiceemailfooter here, which NPE'd at the footer check below
		}
		else if ("notifyprojectadmins".equals(messagetype))
		{
			subject = "Invoice generated at " + librarycol.getName();
			template = template + "invoice-to-notify-team.html";

			invoiceemailheader = (String) librarycol.getValue("invoicepaidemail");
			/*if(invoiceemailheader == null || invoiceemailheader.equals("")) {
				invoiceemailheader =  mediaArchive.getCatalogSettingValue("invoice_notification");
			}*/
			// the original never assigned invoiceemailfooter here, which NPE'd at the footer check below
		}
		else
		{
			subject = null;
			invoiceemailheader = null;
		}

		actionUrl = URLUtilities.urlEscape(actionUrl);

		String supportUrl = actionUrl;
		supportUrl = URLUtilities.urlEscape(supportUrl); // double escape as in the original (unused by the current templates)

		// Other Pay options
		String invoicepayoptions = null;
		String parentprojectinvoicing = (String) librarycol.getValue("parentprojectinvoicing");
		if (parentprojectinvoicing != null)
		{
			Data parentcollectioninvoice = mediaArchive.getCachedData("librarycollection", parentprojectinvoicing);
			if (parentcollectioninvoice != null)
			{
				invoicepayoptions = (String) parentcollectioninvoice.getValue("invoicepayoptions");
			}
		}
		if (invoicepayoptions == null)
		{
			invoicepayoptions = (String) librarycol.getValue("invoicepayoptions");
		}
		if (invoicepayoptions == null)
		{
			invoicepayoptions = mediaArchive.getCatalogSettingValue("invoice_pay_options");
		}

		TemplateWebEmail templateEmail = mediaArchive.createSystemEmail(contact, template);

		// Variables
		objects.put("followeruser", contact);
		objects.put("invoiceto", contact); // change to name

		objects.put("mediaarchive", mediaArchive);
		objects.put("invoice", invoice);
		objects.put("invoicenumber", invoice.getValue("invoicenumber"));
		objects.put("project", librarycol);
		objects.put("supporturl", supportUrl);
		objects.put("actionurl", actionUrl);

		objects.put("siteroot", getSiteRoot(mediaArchive));
		objects.put("librarycol", librarycol);

		String emailname = (String) librarycol.getValue("contactname");

		if (emailname == null)
		{
			emailname = librarycol.getName();
		}

		templateEmail.setFromName(emailname);

		objects.put("community", community); // null when the project has no community tag; ${community} then renders empty
		if (community != null)
		{
			objects.put("communitylink", community.getValue("externaldomain"));
			objects.put("communityhome", "/" + siteid + community.getValue("templatepath"));
		}
		else
		{
			objects.put("communitylink", getSiteRoot(mediaArchive));
			objects.put("communityhome", "/" + siteid);
		}

		// recurring
		objects.put("invoicemonth", month);
		if (((MultiValued) invoice).getDate("duedate") != null)
		{
			String dates = DateStorageUtil.getStorageUtil().formatDateObj(((MultiValued) invoice).getDate("duedate"), "dd/MM/YY");

			objects.put("startdate", dates); // legacy
			objects.put("duedate", dates);
		}
		if (((MultiValued) invoice).getDate("enddate") != null)
		{
			String datee = DateStorageUtil.getStorageUtil().formatDateObj(((MultiValued) invoice).getDate("enddate"), "dd/MM/YY");
			objects.put("enddate", datee);
		}

		// period (the groovy assigned the Data row to a String, which coerces it via toString() = the row name)
		Data periodData = mediaArchive.getData("productrecurringperiod", (String) invoice.getValue("recurringperiod"));
		objects.put("period", periodData != null ? periodData.toString() : null);

		String invoicedescription = mediaArchive.getReplacer().replace((String) invoice.getValue("invoicedescription"), objects);
		objects.put("invoicedescription", invoicedescription);

		String invoicename = mediaArchive.getReplacer().replace(invoice.getName(), objects);
		objects.put("invoicename", invoicename);

		String invoicesubject = mediaArchive.getReplacer().replace(subject, objects);
		templateEmail.setSubject(invoicesubject);

		if (invoiceemailheader != null && !invoiceemailheader.equals(""))
		{
			invoiceemailheader = mediaArchive.getReplacer().replace(invoiceemailheader, objects);
		}
		if (invoiceemailfooter != null && !invoiceemailfooter.equals(""))
		{
			invoiceemailfooter = mediaArchive.getReplacer().replace(invoiceemailfooter, objects);
		}

		objects.put("invoiceheader", invoiceemailheader);
		objects.put("invoicefooter", invoiceemailfooter);

		objects.put("invoicepayoptions", invoicepayoptions);

		// printurl
		String printurl = actionUrl + "/services/printpreview.html?invoiceid=" + invoice.getId() + "&collectionid=" + invoice.getValue("collectionid");
		objects.put("printurl", printurl);

		templateEmail.send(objects);
		log.info(librarycol.getName() + " - Invoice #" + invoice.getValue("invoicenumber") + " - Sent to: " + contact + " Template: " + template);
	}

	private String getSiteId(MediaArchive mediaArchive)
	{
		// the groovy used context.findValue("siteid"), which is null in this deployment; the catalog
		// setting below was the intended app-path override (see the commented-out line in sendinvoice.groovy)
		String siteid = mediaArchive.getCatalogSettingValue("events_billing_notify_invoice_appid");
		if (siteid == null || siteid.length() == 0)
		{
			siteid = "site";
		}
		return siteid;
	}

	private String getSiteRoot(MediaArchive mediaArchive)
	{
		String site = mediaArchive.getCatalogSettingValue("siteroot");
		if (site == null || site.length() == 0)
		{
			site = mediaArchive.getCatalogSettingValue("cdn_prefix");
		}
		return site;
	}

	private InvoiceManager getInvoiceManager()
	{
		return (InvoiceManager) getMediaArchive().getBean("invoiceManager");
	}

	private LocaleManager getLocaleManager()
	{
		return (LocaleManager) getMediaArchive().getBean("localeManager");
	}

}
