package assets

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.util.TimeParser
import org.openedit.Data
import org.openedit.page.Page
import org.openedit.page.manage.PageManager

public void init()
{

	Collection policies = mediaarchive.getList("retentionrules");
	PageManager pageManager = mediaarchive.getPageManager();
	MediaArchive mediaarchive = context.getPageValue("mediaarchive");
	policies.each 
	{
		Map vals = new HashMap();
		Data retentionpolicy = it;
		TimeParser parser = new TimeParser();
		long daystokeep = parser.parse(it.expirationperiod);
		Date target = new Date(System.currentTimeMillis() -  daystokeep);
		 
		Collection assets = mediaarchive.getAssetSearcher().query().exact("retentionpolicy",it.id).not("archivesourcepath", "*").before("assetaddeddate", target).search();
		log.info("Found ${assets.size()} for retention policy ${it} ${assets.query}");
		assets.each
		{
			Asset asset = mediaarchive.getAssetSearcher().loadData(it);
			Page fullpath = mediaarchive.getOriginalFileManager().getOriginalDocument(asset);
			if(fullpath.exists()){
				String mask = retentionpolicy.get("archivepath");
				String newsourcepath = mediaarchive.getAssetImporter().getAssetUtilities().createSourcePathFromMask(context, mediaarchive, asset.getName(), mask, vals);
				
				Page newpage = pageManager.getPage("/WEB-INF/data/" + mediaarchive.getCatalogId() + "/originals/" + newsourcepath);
				pageManager.movePage(fullpath,newpage);
				log.info("Archived asset to ${newpage}");
				asset.setFolder(false);
				asset.setValue("archivesourcepath",newsourcepath);
				mediaarchive.getAssetSearcher().saveData(asset);
			}
			
			
		}
	}
}	
	
init();