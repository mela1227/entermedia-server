/*
 * Created on Oct 3, 2004
 */
package org.openedit.entermedia.xmldb;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.ReferenceMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.openedit.Data;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.AssetArchive;
import org.openedit.entermedia.AssetPathFinder;
import org.openedit.entermedia.Category;
import org.openedit.entermedia.CategoryArchive;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.RelatedAsset;
import org.openedit.repository.ContentItem;
import org.openedit.util.DateStorageUtil;
import org.openedit.xml.XmlArchive;

import com.openedit.ModuleManager;
import com.openedit.OpenEditException;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.users.User;
import com.openedit.util.FileUtils;
import com.openedit.util.XmlUtil;

/**
 * A asset MediaArchive that stores the data about each asset in an
 * <tt>.xconf</tt> file in the <tt>storehome/assets</tt> directory (as
 * determined by its {@link AssetPathFinder}).
 * 
 * @author cburkey
 */
public class XmlAssetArchive extends BaseXmlArchive implements AssetArchive
{
	private static final Log log = LogFactory.getLog(XmlAssetArchive.class);

	protected Map fieldAssets;
	protected PageManager fieldPageManager;
	protected ModuleManager fieldModuleManager;
	protected MediaArchive fieldMediaArchive;
	protected XmlUtil fieldXmlUtil;
	protected boolean fieldUpdateExistingRecord = true;
	
	public XmlAssetArchive()
	{
		log.debug("Created archive");
	}

	protected Map getAssets()
	{
		if (fieldAssets == null)
		{
			// HARD means even if the object goes out of scope we still keep it
			// in the hashmap
			// until the memory runs low then things get dumped randomly
			fieldAssets = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.HARD);
		}
		return fieldAssets;
	}

	public void clearAssets()
	{
		getPageManager().clearCache();
		getAssets().clear();
	}

	public void deleteAsset(Asset inAsset)
	{
		getAssets().remove(inAsset.getSourcePath());
		try
		{
			Page page = getPageManager().getPage(buildXmlPath(inAsset));
			getPageManager().removePage(page);
		}
		catch (Exception ex)
		{
			throw new OpenEditException(ex);
		}
	}

	public void clearAsset(Asset inAsset)
	{
		if (inAsset != null)
		{
			getAssets().remove(inAsset.getSourcePath());
			getPageManager().clearCache(buildXmlPath(inAsset));
		}
	}
	
	public Asset getAssetBySourcePath(String inSourcePath)
	{
		return getAssetBySourcePath(inSourcePath, false);
	}

	public Asset getAssetBySourcePath(String inSourcePath, boolean inAutoCreate)
	{
		if (inSourcePath.endsWith("/"))
		{
			inSourcePath = inSourcePath.substring(0, inSourcePath.length() - 1);
		}
		Asset item = (Asset) getAssets().get(inSourcePath);
		if (item == null)
		{
			item = new Asset();
			item.setCatalogId(getCatalogId());
			item.setSourcePath(inSourcePath);
			String url = buildXmlPath(item);
			if( log.isDebugEnabled())
			{
				log.debug("Loading " + url);
			}
			if( getAssets().size() > 1000)
			{
				clearAssets();
			}
			getAssets().put(inSourcePath, item);
			boolean populated = populateAsset(item);
			if (!populated && !inAutoCreate)
			{
				getAssets().remove(inSourcePath);
				log.debug("No Such asset " + url);
				return null;
			}
		}
		return item;
	}

	public String buildXmlPath(Asset inAsset)
	{
		if (inAsset == null || inAsset.getSourcePath() == null)
		{
			return null;
		}
		
		String prefix = "/WEB-INF/data";
		String catalogId = getCatalogId();
		String path = "/" + catalogId + "/assets/" + inAsset.getSourcePath();
		String 	suffix = "/data.xml";
		
		path = prefix + path + suffix;

		return path;
	}

	/**
	 * This is temporal. Should be deleted soon.
	 * @author Jorge Valencia
	 */
	public String buildOldPath(Asset inAsset)
	{
		if (inAsset == null || inAsset.getSourcePath() == null)
		{
			return null;
		}
		
		String catalogId = getCatalogId();
		String path = "/" + catalogId + "/assets/" + inAsset.getSourcePath();
		String 	suffix = "/_site.xconf";
		
		path = path + suffix;

		return path;
	}
	
	protected boolean populateAsset(Asset inAsset)
	{
		Element asset = null;
		String path = buildXmlPath(inAsset);
		Page assetPage = getPageManager().getPage(path);
		if (!assetPage.exists())
		{
			return false;
		}
		else
		{
			try
			{
				asset = getXmlUtil().getXml(assetPage.getInputStream(), "UTF-8");
//				if(asset.getName().equals("page"))
//				{
//					asset = asset.element("product");
//				}
			}
			catch ( Exception ex )
			{
				log.error("Could not load: " + assetPage.getPath(), ex );
				
				return false;
			}
		}

		if (asset == null)
		{
			return false;
		}
		
		boolean ok =  populateAsset(inAsset, asset);
		if( inAsset.getId() == null)
		{
			throw new OpenEditException("Id is required " + assetPage.getPath());
		}
		return ok;
	}
	
	protected boolean populateAsset(Asset inAsset, Element inAssetElement) 
	{
		String name = inAssetElement.attributeValue("name");
		inAsset.setName(name);

		String order = inAssetElement.attributeValue("ordering");
		if (order != null && order.length() > 0)
		{
			inAsset.setOrdering(Integer.parseInt(order));
		}
		String id = inAssetElement.attributeValue("id");
		inAsset.setId(id);

		loadCategories(inAsset, inAssetElement);
		List children = inAssetElement.elements("keyword");
		
		List<String> keywords = new ArrayList<String>();
		if (children == null || children.size() > 0)
		{
			for (Object o: children)
			{
				Element keyword = (Element)o;
				keywords.add(keyword.getTextTrim());
			}
		}
		else
		{
			Element keywordslist = inAssetElement.element("keywords");
			if (keywordslist != null)
			{
				keywords.add(keywordslist.getTextTrim());
			}
		}
		inAsset.setKeywords(keywords);

		for (Object o: inAssetElement.elements("property"))
		{
			Element prop = (Element) o;
			String key = prop.attributeValue("name");
			if("originalfilename".equals(key) || "primaryimagename".equals(key) || "originalattachment".equals(key))
			{
				key = "primaryfile";
			}
			inAsset.setProperty(key, prop.getTextTrim());
		}
		Page originalPage = getPageManager().getPage("/WEB-INF/data/" + inAsset.getCatalogId() + "/originals/" + inAsset.getSourcePath());
		inAsset.setFolder(originalPage.isFolder());
		loadRelatedAssets(inAsset, inAssetElement);
		return true;
	}

	protected void loadRelatedAssets(Asset inAsset, Element inAssetConfig)
	{
		inAsset.clearRelatedAssets();
		Element relatedAssetsElem = inAssetConfig.element("related-assets");
		if (relatedAssetsElem != null)
		{
			for (Object o: relatedAssetsElem.elements("asset"))
			{
				Element relatedAssetConfig = (Element) o;
				RelatedAsset related = new RelatedAsset();
				related.setAssetId(inAsset.getId());

				String id = relatedAssetConfig.attributeValue("id");
				related.setRelatedToAssetId(id);

				String type = relatedAssetConfig.attributeValue("type");
				related.setType(type);

				String catid = relatedAssetConfig.attributeValue("catalogid");
				if (catid == null)
				{
					catid = getCatalogId();
				}
				related.setRelatedToCatalogId(catid);
				inAsset.addRelatedAsset(related);
			}
		}
	}


	public void saveAsset(Asset inAsset)
	{
		saveAsset(inAsset, (User) null);
	}
	public void saveData(Data inData, User inUser)
	{
		saveAsset((Asset)inData, inUser);
	}
	public synchronized void saveAsset(Asset inAsset, User inUser)
	{
		if( inAsset.getId() == null)
		{
			throw new OpenEditException("Saved asset has null ID");
		}
		if(inAsset.getSourcePath() == null)
		{
			throw new OpenEditException("asset does not have a sourcepath " + inAsset);
		}
		try
		{
			// TODO: Speed check this section
			// TODO: Force users to set the sourcePath if it is not set
			String path = buildXmlPath(inAsset);
			Page output = getPageManager().getPage(path);
			String encoding = "UTF-8";
			Document document = DocumentHelper.createDocument();
			document.setRootElement(DocumentHelper.createElement("asset"));
			saveAsset(inAsset, document.getRootElement());
			// save it to disk
			//OutputStreamItem xconf = new OutputStreamItem(path);// , new
			// ElementReader(rootElement,getXmlUtil().getWriter(encoding)),
			// encoding);
			//xconf.setMakeVersion(false);
			//getPageManager().getPageSettingsManager().saveSetting(xconf); // This
			// sets
			// the
			// output
			// stream
			
			OutputStream out = getPageManager().saveToStream(output);
			try
			{
				getXmlUtil().saveXml(document.getRootElement(), out, encoding);
			}
			finally
			{
				FileUtils.safeClose(out);
			}
			if( getAssets().size() > 1000)
			{
				clearAssets();
			}
			getAssets().put(inAsset.getSourcePath(), inAsset);

		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			String msg = "Saving asset " + inAsset.getSourcePath() + " failed: ";
			log.error(msg + ex);
			throw new OpenEditException(msg + ex, ex);
		}
	}

	public void saveAsset(Asset inAsset, Element assetelm)
	{
		saveAssetAttributes(inAsset, assetelm);

		// save out catalogs
		saveAssetCatalogs(inAsset, assetelm);


		// clear out any old asset properties
		deleteElements(assetelm, "property");
		// saves out properties
		PropertyDetails details = getMediaArchive().getAssetPropertyDetails();
		PropertyDetail moddetail = details.getDetail("recordmodificationdate");
		if (moddetail != null )
		{
			String recordmod = DateStorageUtil.getStorageUtil().formatForStorage(new Date());
			inAsset.setProperty("recordmodificationdate", recordmod);
		}

		for (Iterator iter = inAsset.getProperties().keySet().iterator(); iter.hasNext();)
		{
			String key = (String) iter.next();
			if( key.startsWith("_"))
			{
				continue;
			}
			PropertyDetail detail = details.getDetail(key);
			String value = inAsset.getProperty(key);

			if (value != null && value.length() > 0)
			{
				value = value.trim();
				Element newProperty = assetelm.addElement("property");
				newProperty.addAttribute("name", key);
				// newProperty.addAttribute("id", key);

				if (detail != null && detail.isViewType("html"))
				{
					newProperty.addCDATA(value);
				}
				else
				{
					if (value.contains("  "))
					{
						newProperty.addCDATA(value);
					}
					else
					{
						newProperty.setText(value);
					}
				}
			}
		}
		saveRelatedAssets(inAsset, assetelm);
	}

	protected boolean hasProperty(Element inRootElement, String inName)
	{
		for (Iterator iter = inRootElement.elementIterator("property"); iter.hasNext();)
		{
			Element element = (Element) iter.next();
			String name = element.attributeValue("name");
			if (inName.equals(name))
			{
				return true;
			}
		}
		return false;
	}

	private Document loadXconfDocument(ContentItem item, String encode) throws OpenEditException, DocumentException, IOException
	{
		Document document;

		// String url = PRODUCTS_URL_PATH + "/" +
		// getAssetPathFinder().idToPath(inId) + ".html";

		if (item.exists())
		{
			document = getXmlUtil().getXml(item.getInputStream(), encode).getDocument();
		}
		else
		{
			document = DocumentHelper.createDocument();
			document.setRootElement(DocumentHelper.createElement("page"));
		}
		return document;
	}

	protected void saveAssetAttributes(Asset inAsset, Element assetelm)
	{
		String name = inAsset.getName();
		// name = SpecialCharacter.escapeSpecialCharacters(name);
		assetelm.addAttribute("name", name);
		assetelm.addAttribute("id", inAsset.getId());
		if (inAsset.getOrdering() > 0)
		{
			assetelm.addAttribute("ordering", String.valueOf(inAsset.getOrdering()));
		}
		saveKeywords(inAsset, assetelm);
	}

	protected void saveKeywords(Asset inAsset, Element inAssetElment)
	{
		if (inAsset.getKeywords() != null)
		{
			deleteElements(inAssetElment, "keywords"); // legacy
			deleteElements(inAssetElment, "keyword");
			for (Iterator iter = inAsset.getKeywords().iterator(); iter.hasNext();)
			{
				String val = (String) iter.next();
				Element keywordElement = inAssetElment.addElement("keyword");
				keywordElement.addCDATA(val); // This will dodge invalid
				// character problems
				// keywordElement.setText(val); //This may give us an exception
				// with invalid characters
			}
		}
	}

	/**
	 * @param inAsset
	 * @param assetelm
	 */
	private void saveAssetCatalogs(Asset inAsset, Element assetelm)
	{
		// remove old catalogs
		deleteElements(assetelm, "category");
		// TODO: Save over the old category
		for (Iterator iter = inAsset.getCategories().iterator(); iter.hasNext();)
		{
			Category category = (Category) iter.next();
			Element cat = assetelm.addElement("category");
			cat.addAttribute("id", category.getId());
		}
	}

	protected void saveRelatedAssets(Asset inAsset, Element inAssetElement)
	{
		deleteElements(inAssetElement, "related-assets");
		if (inAsset.hasRelatedAssets())
		{
			Element relatedAssetsElem = inAssetElement.addElement("related-assets");
			for (Iterator iter = inAsset.getRelatedAssets().iterator(); iter.hasNext();)
			{
				RelatedAsset related = (RelatedAsset) iter.next();
				Element e = relatedAssetsElem.addElement("asset");
				e.addAttribute("id", related.getRelatedToAssetId());
				String catid = related.getRelatedToCatalogId();
				e.addAttribute("catalogid", catid);
				e.addAttribute("type", related.getType());
			}
		}
	}

	protected void loadCategories(Asset inAsset, Element inAssetConfig)
	{
		inAsset.clearCategories();
		for (Object o: inAssetConfig.elements("category"))
		{
			Element categoriesConfig = (Element) o;
			String catid = categoriesConfig.attributeValue("id");
			Category category = getCategoryArchive().getCategory(catid);
			if (category == null)
			{
				log.error("Could not find a category with id: " + catid);
				continue;
			}
			inAsset.addCategory(category);
		}
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}


	protected CategoryArchive getCategoryArchive()
	{
		return getMediaArchive().getCategoryArchive();
	}

	public XmlUtil getXmlUtil()
	{
		if (fieldXmlUtil == null)
		{
			fieldXmlUtil = new XmlUtil();
		}
		return fieldXmlUtil;
	}

	public void setXmlUtil(XmlUtil inUtil)
	{
		fieldXmlUtil = inUtil;
	}

	public boolean isUpdateExistingRecord()
	{
		return fieldUpdateExistingRecord;
	}

	public void setUpdateExistingRecord(boolean inUpdateExistingRecord)
	{
		fieldUpdateExistingRecord = inUpdateExistingRecord;
	}


	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	public MediaArchive getMediaArchive()
	{
		if (fieldMediaArchive == null)
		{
			fieldMediaArchive = (MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");
		}
		return fieldMediaArchive;
	}

	@Override
	public void setXmlArchive(XmlArchive inXmlArchive)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDataFileName(String inDataFileName)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setElementName(String inSearchType)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setPathToData(String inPathToData)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public XmlArchive getXmlArchive()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void delete(Data inData, User inUser)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void saveAllData(Collection<Data> inAll, User inUser)
	{
		for (Iterator iterator = inAll.iterator(); iterator.hasNext();)
		{
			Asset asset = (Asset) iterator.next();
			saveAsset(asset, inUser);
		}
		
	}

}
