package modules;

import org.openedit.Data
import org.openedit.data.*
import org.entermediadb.asset.*
import org.openedit.hittracker.SearchQuery
import org.openedit.hittracker.HitTracker
import org.openedit.page.Page
import org.openedit.WebPageRequest


public void init(){
	
	MediaArchive mediaarchive = context.getPageValue("mediaarchive");
	
	Data currrentversion = mediaarchive.query("desktopversions").all().sort("orderingDown").searchOne();
	
	log.info("Found " + currrentversion);
	
	
	String localversion = context.getRequestParameter("localversion");
	String newversion = currrentversion.get("versionnumber");
	boolean needsupgrade  =true;
	if( newversion.equals(localversion))
	{
		needsupgrade = false;
	}
	context.putPageValue("needsupgrade", needsupgrade); 
	context.putPageValue("currrentversion", currrentversion);
 	context.putPageValue("localversion",localversion);
	   log.info("Done" + currrentversion.get("versionnumber"));
}

init();

