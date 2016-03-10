package quickshare;

import org.entermedia.email.PostMail
import org.entermedia.email.TemplateWebEmail
import org.entermedia.upload.FileUpload
import org.entermedia.upload.FileUploadItem
import org.entermedia.upload.UploadRequest
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.*
import org.openedit.entermedia.creator.*
import org.openedit.entermedia.edit.*
import org.openedit.entermedia.episode.*
import org.openedit.entermedia.modules.*
import org.openedit.entermedia.orders.Order
import org.openedit.entermedia.orders.OrderSearcher
import org.openedit.entermedia.util.*
import org.openedit.util.DateStorageUtil
import org.openedit.xml.*

import com.openedit.WebPageRequest
import com.openedit.hittracker.*
import com.openedit.page.*
import com.openedit.users.User
import com.openedit.util.*


public void handleUpload() {
	MediaArchive archive = (MediaArchive)context.getPageValue("mediaarchive");
	FileUpload command = archive.getSearcherManager().getModuleManager().getBean("fileUpload");
	UploadRequest properties = command.parseArguments(context);
	OrderSearcher ordersearcher = archive.getSearcher("order");
	Searcher itemsearcher = archive.getSearcher("orderitem");
	
	if (properties == null) {
		return;
	}
	if (properties.getFirstItem() == null) {
		return;
	}
	User user = context.getUser();
	if(user == null){
		user = archive.getUserManager().createGuestUser("anonymous", "anonymous", "anonymous");
		user.setVirtual(true);
	}
	
	Order order = context.getSessionValue("quickshareorder");
	if(order == null){
		 order = ordersearcher.createNewData();
		 order.setId(ordersearcher.nextId());
	
	}
	order.setProperty("orderstatus", "complete");
	order.setProperty("publishdestination", "0");
	order.setProperty("applicationid", context.findValue("applicationid"));
	String sharenote = context.findValue("sharenote.value");
	order.setProperty("sharenote", sharenote);
	ordersearcher.saveData(order, null);
	context.putSessionValue("quickshareorder", order);
	
	List orderitems = new ArrayList();
	properties.getUploadItems().each{
		FileUploadItem file = it;
		
		String sourcepath = "submitted/" + context.getUserName() + "/${file.getName()}";
		Asset current = archive.getAssetBySourcePath(sourcepath);
		if(current ==  null){
			current = archive.createAsset(sourcepath);
		}
		current.setProperty("assetaddeddate", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
		String path = "/WEB-INF/data/" + archive.getCatalogId()	+ "/originals/" + sourcepath + "/${file.getName()}";
		String[] fields = context.getRequestParameters("field");
		if(fields != null){
			archive.getAssetSearcher().updateData(context, fields, current);
		}
		properties.saveFileAs(file, path, user);
		current.setPrimaryFile(file.getName());
		current.setProperty("owner", context.getUserName());
		current.setProperty("userprofile", context.getUserProfile().getId());
		current.setProperty("submittedfile", "true");
		archive.saveAsset(current, null);
		Data orderitem = itemsearcher.createNewData();
		orderitem.setProperty("orderid", order.getId());
		orderitem.setProperty("assetid", current.getId());
		
		orderitem.setProperty("publishqueueid", "0");
		
		orderitem.setProperty("presetid", "0");
		
		orderitem.setProperty("assetsourcepath", current.getSourcePath());
		orderitem.setProperty("presetid", "original");
		orderitem.setProperty("sharenote", context.getRequestParameter("sharenote.value"));
		orderitems.add(orderitem);
		
					
	
	}
	
	itemsearcher.saveAllData(orderitems, null);
	context.putPageValue("orderitems", orderitems);
	String from = context.getRequestParameter("email.value");
	context.putPageValue("fromemail", from);
	context.putPageValue("order", order);
	String to = context.getRequestParameter("destination.value");
	String sendfrom = context.findValue("quicksharefrom");
	//sendEmail(context,  to,sendfrom, "/${context.findValue('applicationid')}/components/quickshare/sharetemplate.html");
	//sendEmail(context,  from,sendfrom, "/${context.findValue('applicationid')}/components/quickshare/sharetemplate.html");
	

}

protected void sendEmail(WebPageRequest context, String email, String from,  String templatePage){
	Page template = pageManager.getPage(templatePage);
	WebPageRequest newcontext = context.copy(template);
	TemplateWebEmail mailer = getMail();
	mailer.loadSettings(newcontext);
	mailer.setFrom(from);
	mailer.setMailTemplatePath(templatePage);
	mailer.setRecipientsFromCommas(email);
	mailer.send();
	log.info("conversion error email sent to ${email}");
}

protected TemplateWebEmail getMail() {
	PostMail mail = (PostMail)mediaarchive.getModuleManager().getBean( "postMail");
	return mail.getTemplateWebEmail();
}

handleUpload();
