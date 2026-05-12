import org.entermediadb.asset.MediaArchive
import org.entermediadb.email.PostMail
import org.entermediadb.email.TemplateWebEmail
import org.openedit.BaseWebPageRequest
import org.openedit.Data
import org.openedit.data.BaseSearcher
import org.openedit.users.User
import org.openedit.util.RequestUtils


public void init() {

	//Notify to Email:
	//String notifyemail = "sales@entermediadb.org";  //get it from catalog settings?
	
	String notifyemail = "sales@entermediadb.org";  //get it from catalog settings?

	//prevent re-submition
	/* String clientform = context.getSessionValue("clientform");
	 if (clientform != null) {
	 	context.putSessionValue("clientform", null);
	 }
	 else {
	 	context.putPageValue("errorcode", "1");
	 	log.info("Form re-submit prevented");
	 	return;
	 }
*/
	MediaArchive archive = context.getPageValue("mediaarchive");
	Boolean valid = true;
	String field1 = context.getRequestParameter("username"); //Fake fields for spamm control
	if(field1 != null) {
		valid = false;
	}
	String field2 = context.getRequestParameter("email");
	if(field2 != null) {
		valid = false;
	}
	if(!valid) {
		log.info("Invalid form request. Possible Spam.");
		return;
	}
	
	User user = context.getUser();
	
	if (user == null)
	{
		return;
	}
	
	String requiredlibraryname = context.getRequestParameter("projectname");
	
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	
	
	//Create Library
	BaseSearcher collectionsearcher = mediaArchive.getSearcher("librarycollection");
	
	Data newcollection =  collectionsearcher.createNewData();
	newcollection.setValue("name", requiredlibraryname);
	newcollection.setValue("owner", user.getId());
	newcollection.setValue("library","clientcollections");
	newcollection.setValue("communitytagcategory","emediawebsite");
	newcollection.setValue("collectiontype","6"); //Client Library
	newcollection.setValue("serverstatus","requested");
	newcollection.setValue("creationdate", new Date());
	collectionsearcher.saveData(newcollection);

	log.info("User collection created: " + newcollection.getId());
	
	//add user to librarycollectionusers
	BaseSearcher lcusearcher = mediaArchive.getSearcher("librarycollectionusers");
	Data lcu = lcusearcher.createNewData();
	lcu.setValue("collectionid",newcollection.getId());
	lcu.setValue("followeruser",user.getId());
	lcu.setValue("ontheteam",true);
	lcusearcher.saveData(lcu);
	
	
	//add client Support local user agent
		
	String localclientsupportuser = "105";  //CB
	Data lcu2 = lcusearcher.createNewData();
	lcu2.setValue("collectionid",newcollection.getId());
	lcu2.setValue("followeruser", localclientsupportuser);
	lcu2.setValue("ontheteam",true);
	lcusearcher.saveData(lcu2);
	
	
	//Notify Email
	context.putPageValue("subject", "Library Created in Website");
	HashMap htmlfields = new HashMap();
	htmlfields.put("user", user.getScreenName());
	htmlfields.put("useremail", user.getEmail());
	htmlfields.put("libraryname", requiredlibraryname);
	htmlfields.put("currentproject", context.getRequestParameter("project"));
	
	//Message requester info
	context.putPageValue("messagetime", new Date() );
	context.putPageValue("fields", htmlfields);
	
	//Message requester info
	context.putPageValue("messagetime", new Date() );
	String ipaddress = context.getRequest().getRemoteAddr();
	String senderinfo = "Url: "+context.getPageValue("siteroot");
	if (context.getPageValue("referringPage") != null) {
		senderinfo = senderinfo + " Refering page: "+context.getPageValue("referringPage");
	}
	if (context.getPageValue("page") != null) {
		senderinfo = senderinfo + " Page: "+context.getPageValue("page");
	}
	senderinfo = senderinfo + " Ip: " + ipaddress;
	context.putPageValue("senderinfo",   senderinfo);
	
	String communityhome = context.getPageValue("communityhome");
	String templateSrc = communityhome+"/account/createlibrarynotifyemail.html";
	sendEmail(context.getPageMap(), notifyemail, templateSrc);
}






protected void sendEmail(Map pageValues, String email, String templatePage){
	//send e-mail
	//Page template = getPageManager().getPage(templatePage);
	User user = context.getUser();
	RequestUtils rutil = moduleManager.getBean("requestUtils");
	BaseWebPageRequest newcontext = rutil.createVirtualPageRequest(templatePage, user, null);
	
	newcontext.putPageValues(pageValues);

	PostMail mail = (PostMail)moduleManager.getBean("postMail");
	TemplateWebEmail mailer = mail.getTemplateWebEmail();
	mailer.loadSettings(newcontext);
	mailer.setMailTemplatePath(templatePage);
	mailer.setRecipientsFromCommas(email);
	mailer.send();
	log.info("email sent to ${email}");
}

init();


