import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.data.BaseSearcher
import org.openedit.users.User

public void init()
{
	User user = context.getUser();
	
	if (user == null)
	{
		return;
	}

	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	
	BaseSearcher collectionsearcher = mediaArchive.getSearcher("librarycollection");
	
	Data usercollection = mediaarchive.getCachedData("librarycollection", "account"+ user.getId());
	
	log.info("User collection: " + usercollection);
	
	if(usercollection == null) {
		//create collection
		usercollection = collectionsearcher.createNewData();
		usercollection.setId("account" + user.getId());
		usercollection.setValue("name", user.getName());
		usercollection.setValue("owner", user.getId());
		usercollection.setValue("library","userscollections");
		usercollection.setValue("communitytagcategory","emediawebsite");
		usercollection.setValue("collectiontype","7"); //User Library
		collectionsearcher.saveData(usercollection);

		log.info("User collection created: " + usercollection.getId());
		
		//add to librarycollectionusers
		BaseSearcher lcusearcher = mediaArchive.getSearcher("librarycollectionusers");
		Data lcu = lcusearcher.createNewData();
		lcu.setValue("collectionid",usercollection.getId());
		lcu.setValue("followeruser",user.getId());
		lcu.setValue("ontheteam",true);
		lcusearcher.saveData(lcu);
		
		//add client Support local user agent
		
		String localclientsupportuser = "105";
		Data lcu2 = lcusearcher.createNewData();
		lcu2.setValue("collectionid",usercollection.getId());
		lcu2.setValue("followeruser", localclientsupportuser);
		lcu2.setValue("ontheteam",true);
		lcusearcher.saveData(lcu2);
		
		
		//Send Welcome Message
		BaseSearcher cbsearcher = mediaArchive.getSearcher("chatterbox");
		Data chat = cbsearcher.createNewData();
		chat.setValue("message", "Hi there! I'm your account manager here at eMedia Library. How can I assist you today? Whether you have questions about our services, need help with your account, I'm here to help. Feel free to ask me anything.");
		chat.setValue("user", localclientsupportuser);
		chat.setValue("collectionid", usercollection.getId());
		chat.setValue("channel", "supportchat" + user.getId());
		Date now = new Date();
		chat.setValue("date", now);
		cbsearcher.saveData(chat);
		
	}
	else {
		 
	}
	context.putPageValue("librarycol", usercollection);
	context.putPageValue("collectionid", usercollection.getId());
	
	//topic
	String defaultusertopic = "supportchat" + user.getId();
	BaseSearcher topicsearcher = mediaArchive.getSearcher("collectiveproject");
	Data currenttopic = topicsearcher.query().exact("id", defaultusertopic).searchOne();
	if (currenttopic == null) {
		currenttopic = topicsearcher.createNewData();
		currenttopic.setValue("parentcollectionid",usercollection.getId())
		currenttopic.setName("Support Chat");
		currenttopic.setId(defaultusertopic);
		topicsearcher.saveData(currenttopic)
	}
	context.putPageValue("currenttopic", currenttopic);

}


init();