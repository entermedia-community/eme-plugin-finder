import org.entermediadb.asset.MediaArchive
import org.entermediadb.location.Position
import org.entermediadb.projects.*
import org.openedit.Data

import org.openedit.data.BaseSearcher
import org.openedit.data.*

public void init()
{
	MediaArchive mediaarchive = context.getPageValue("mediaarchive");
	
	Data communitytagcategory = context.getPageValue("communitytagcategory");
	
	QueryBuilder query = mediaarchive.query("librarycollection").exact("communitytagcategory",communitytagcategory.getId()).named("collections");
	Collection collections = query.search(context);
	if( !collections.isEmpty() )
	{
		query = mediaarchive.query("collectiveproduct").hitsPerPage(20).orgroup("collectionid",collections).exact("producttype","5").named("products").sort("name");
		
		String indate = context.getRequestParameter("checkIn");
		if( indate != null)
		{
			query.not("blockeddates",indate); 
			query.exact("fullybooked","false");
		}
		
		Collection hits = query.search(context);
	    context.putPageValue("hits",hits);
	}
	
}

init();