package org.entermediadb.asset.convert.managers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.convert.BaseConversionManager;
import org.entermediadb.asset.convert.ConversionManager;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.entermediadb.asset.convert.TranscodeTools;
import org.openedit.Data;
import org.openedit.page.Page;

public class DefaultConversionManager extends BaseConversionManager
{
	private static final Log log = LogFactory.getLog(DefaultConversionManager.class);


	
	public ConvertResult transcode(ConvertInstructions inStructions)
	{
		//if output == jpg and no time offset - standard
		
		
		String mime = getMediaArchive().getMimeTypeIcon(inStructions.getAsset().getFileFormat());
		String thumbpath = inStructions.get("themeprefix") + "/images/mimetypes/" +mime  + ".png";

		Page page = getMediaArchive().getPageManager().getPage(thumbpath);
		
		
		
		TranscodeTools creatorManager = getMediaArchive().getTranscodeTools();
		ConversionManager transcoder = creatorManager.getManagerByRenderType("image");
		
		//ConvertInstructions cacheInsructions = transcoder.createInstructions();
		ConvertInstructions cacheInsructions = new ConvertInstructions(getMediaArchive());
		cacheInsructions.setMaxScaledSize(inStructions.getMaxScaledSize());
		cacheInsructions.setInputFile(page.getContentItem());
		String width = String.valueOf(inStructions.getMaxScaledSize().getWidth());
		String height = String.valueOf(inStructions.getMaxScaledSize().getWidth());
		String outpath = inStructions.get("themeprefix") + "/images/mimetypes/" +mime  + width + "x" + height + ".png";
		Page out = getMediaArchive().getPageManager().getPage(outpath);
		if(!out.exists()){
    	cacheInsructions.setOutputFile(out.getContentItem());
		
    	ConvertResult result = transcoder.createOutput(cacheInsructions);
    	return result;
		}
		
		
		ConvertResult result = new ConvertResult();
		result.setOutput(out.getContentItem());
		result.setOk(true);
		result.setComplete(true);
		return result;
		
		
		
		
	}
	
	


	@Override
	protected String getRenderType()
	{
		return "default";
	}

}