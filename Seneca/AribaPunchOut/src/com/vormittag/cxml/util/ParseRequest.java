package com.vormittag.cxml.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;
import org.jdom.Content;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import com.vormittag.util.AribaOrder;
import com.vormittag.util.AribaOrderDetails;
import com.vormittag.util.AribaOrderDetailsLineNumberComparator;
import com.vormittag.util.ConfigCachePunchoutAriba;
import com.vormittag.util.ExceptionUtils;
import com.vormittag.util.OrderDetail;
import com.vormittag.util.OrderHeader;
import com.vormittag.util.PunchoutResponse;
import com.vormittag.util.SqlUtil;
import com.vormittag.was.util.AribaDbRequest;
import com.vormittag.was.util.Item;
import com.vormittag.was.util.ShippingOption;
import com.vormittag.was.util.UnitOfMeasure;


/**
 * @author johnv
 */
public class ParseRequest {
	private static final String cName = ParseRequest.class.getCanonicalName();
	private static Logger log = Logger.getLogger(cName);

	/** Default SAX Driver class to use */
	private static final String DEFAULT_SAX_DRIVER_CLASS = "org.apache.xerces.parsers.SAXParser";

	/** URL for accepting PunchOutSetupRequests */
	private static final String PUNCHOUT_URL = "https://?/AribaSupplier/AribaPunchOutRequest";

	/** Which PunchOut operations do we support? */
	private static final String OPERATION_ALLOWED =
			// "create edit inspect";
			"create";

	/** URL for accepting Order_Requests */
	private static final String ORDER_URL =
			"https://?/AribaSupplier/AribaPunchOutRequest";

	/** Do we accept change and delete orders? */
	private static final String ORDER_CHANGES =
			"No";

	/** Do we accept order attachments ? */
	private static final String ORDER_ATTACHMENTS =
			"No";

	/** SAX Driver Class to use */
	private String saxDriverClass;

	/** <code>{@link SAXBuilder}</code> instance to use */
	private SAXBuilder builder;

	// Session variable to use in puchOutRequest
	private String session;
	
	private PunchoutResponse response = null;

	/*
	 * I can instantiate the class with with the following
	 * mySaxDriverClass and set validation on/off
	 * theDefaultSaxDriverClass defined above and validation on/off
	 */
	public ParseRequest(String saxDriverClass, boolean validate,String session) {
		String cMethod = "Constructor specifying driver";
		log.entering(cName, cMethod);
		this.session = session;
		this.saxDriverClass = saxDriverClass;
		builder = new SAXBuilder(saxDriverClass, validate); // validation on
		log.exiting(cName, cMethod);
	}

	public ParseRequest(boolean validate,String session) {
		String cMethod = "Constructor default driver";
		log.entering(cName, cMethod);
		this.session = session;
		try{
			this.saxDriverClass = DEFAULT_SAX_DRIVER_CLASS;
			builder = new SAXBuilder(saxDriverClass, validate);
		}
		catch(Exception e) {
			log.severe("Parse Error: "+e.getMessage() + "\n" + ExceptionUtils.getStackTrace(e));
		}
		log.exiting(cName, cMethod);
	}

	/**
	 * <p>
	 * This will parse the specified input stream using SAX and the
	 *   SAX driver class specified in the constructor.
	 * </p>
	 *
	 * @param in <code>InputStream</code> input stream to parse.
	 * @param out <code>OutputStream</code> to output to.
	 * Outputstream is passed by reference
	 */
	public void read(InputStream in, PrintStream out) throws IOException, JDOMException {
		String cMethod = "read (IO)";
		log.entering(cName, cMethod);
		// Build the JDOM Document
		Document doc = builder.build(in);
		this.read(doc, out);
		log.exiting(cName, cMethod);
	}

	/**
	 * <p>
	 * This is where the real work is done...
	 * We authenticate the credentials in the header, process the request
	 * and send the appropriate response
	 * </p>
	 *
	 * @param doc <code>org.jdom.Document</code> the request JDOM Document
	 * @param out <code>OutputStream</code> to output to.
	 */
	public void read(Document doc, PrintStream out)	throws IOException, JDOMException
	{
		String cMethod = "read (DO)";
		log.entering(cName, cMethod);
		log.finest("CXML IN:");
		log.finest(new XMLOutputter().outputString(doc));
		
		Document response = null;
		
		try
		{	
			ConfigCachePunchoutAriba cc = ConfigCachePunchoutAriba.getInstance();
			
			// Get the root element
			Element root = doc.getRootElement();
			
			// Get Header Info
			String from = root.getChild("Header").getChild("From").getChild("Credential").getChild("Identity").getTextTrim();
			String secret = root.getChild("Header").getChild("Sender").getChild("Credential").getChild("SharedSecret").getTextTrim();
			String domain = root.getChild("Header").getChild("Sender").getChild("Credential").getAttribute("domain").getValue();
			log.fine("From=" + from + ", secret=" + secret + ", domain=" + domain);
			// Authenticate
			if (!secret.equals(cc.getAribaSharedSecret()))
			{
				response = createResponse(getTimestamp(),getPayloadId(), "401", "Unauthorized - wrong password");
			} 
			else if (!lookupBuyer(domain, from))
			{
				response = createResponse(getTimestamp(),getPayloadId(), "401", "Unauthorized - not a valid buyer");
			} 
			else
			{
				// Get the request type
				String requestType = ((Element)root.getChild("Request").getChildren().get(0)).getName();
				
				if(requestType.equals("PunchOutSetupRequest"))
				{
					//Set the buyer cookie first
					String buyerCookie = root.getChild("Request").getChild("PunchOutSetupRequest").getChild("BuyerCookie").getTextTrim();
					String postUrl = root.getChild("Request").getChild("PunchOutSetupRequest").getChild("BrowserFormPost").getChild("URL").getTextTrim();
					
					String addressID = null;
					try
					{
						addressID = root.getChild("Request").getChild("PunchOutSetupRequest").getChild("ShipTo").getChild("Address").getAttributeValue("addressID");
						//if it is blank, we'll also consider it unassigned
						if (addressID == null || addressID.length()==0)
						{
							addressID = AribaDbRequest.DEFAULT_ADDRESS_ID;
						}
					}
					catch (NullPointerException n)
					{
						addressID = AribaDbRequest.DEFAULT_ADDRESS_ID;
					}
					
					String networkID = root.getChild("Header").getChild("From").getChild("Credential").getChild("Identity").getTextTrim();
					log.finest("About to check the address id");
					//Check if I get a hit for the address id here
					AribaDbRequest adr = new AribaDbRequest(cc.getAribaCompany(),cc.getAribaWebsiteId(),cc.getJndiName(),cc.getProviderURL(),session,cc.getAribaUser(),buyerCookie);
					if(adr.checkAddressId(addressID,networkID))
					{
						log.finest("I have a good address id");
						response = handlePunchOutRequest(cc,root,buyerCookie,postUrl,addressID,networkID);
					}
					/*// If there is a default address id for this account, allow the request to proceed as if we found a match on the address id.
					else if (adr.checkAddressId(AribaDbRequest.DEFAULT_ADDRESS_ID, networkID))
					{
						response = handlePunchOutRequest(cc,root,buyerCookie,postUrl,AribaDbRequest.DEFAULT_ADDRESS_ID,networkID);
						log.info("Inbound Ariba Transaction with network id: " + networkID + " and address id: " + addressID + " was allowed to proceed because a default address id was found.");
					}*/
					else
					{
						response = createResponse(getTimestamp(),getPayloadId(), "700", "Error - Customer is not currently configured to order from " + cc.getDomain() + " . "+
								"Please contact " + cc.getDomain() + " : "+requestType);
					}
				}
				else if (requestType.equals("OrderRequest"))
				{
					log.finest("In order request. Parsing  xml for address and network ids");
					
					String addressID = null;
					try
					{
						addressID = root.getChild("Request").getChild("OrderRequest").getChild("OrderRequestHeader").getChild("ShipTo").getChild("Address").getAttributeValue("addressID");
					}
					catch (NullPointerException n)
					{
						addressID = AribaDbRequest.DEFAULT_ADDRESS_ID;
					}
					log.finest("addressId :: "+addressID);
					
					String networkID = root.getChild("Header").getChild("From").getChild("Credential").getChild("Identity").getTextTrim();
					log.finest("networkID :: "+networkID);

					//Check if I get a hit for the address id here
					AribaDbRequest adr = new AribaDbRequest(cc.getJndiName(),cc.getProviderURL(),session,cc.getAribaUser()); 
					if (adr.checkAddressId(addressID,networkID)) {
						log.finest("addressId Hit!");
						//String buyerCookie = root.getChild("Request").getChild("PunchOutSetupRequest").getChild("BuyerCookie").getTextTrim();
						//String postUrl = root.getChild("Request").getChild("PunchOutSetupRequest").getChild("BrowserFormPost").getChild("URL").getTextTrim();
						response = handleOrderRequest(root, addressID, networkID);
					}
					/*else if (adr.checkAddressId(AribaDbRequest.DEFAULT_ADDRESS_ID, networkID))
					{
						log.finest("default addressId Hit!");
						response = handleOrderRequest(root, AribaDbRequest.DEFAULT_ADDRESS_ID, networkID);
					}*/
					else {
						log.finest("addressId NOT Hit!");
						response = createResponse(getTimestamp(),getPayloadId(), "700", "Error - Customer is not currently configured to order from " + cc.getDomain()
								+ " . " + "Please contact " + cc.getDomain() + " : "+requestType);
					}
					log.finest("Just got response");
				}
				else if(requestType.equals("ProfileRequest")) {
					response = handleProfileRequest(root);
				} else if (requestType.equals("InvoiceDetailRequest")){
					log.fine("InvoiceDetailRequest");
					response = handleInvoiceRequest(root);
					log.fine("got invoice response");
				} else {
					response = createResponse(getTimestamp(),getPayloadId(), "500", "Error - Unknown request: "+requestType);
				}

			}
			// Send Response
			sendResponse (response, out);
		} catch(Exception e) {
			log.severe("Exception from read: "+e.getMessage() + "\n" + ExceptionUtils.getStackTrace(e));
		}
		log.exiting(cName, cMethod);
	}
	
	public void sendResponse (Document doc, OutputStream out) throws IOException, JDOMException {
		String cMethod = "sendResponse";
		log.entering(cName, cMethod);

		XMLOutputter fmt = new XMLOutputter();
		fmt.output(doc, out);

		log.exiting(cName, cMethod);
	}

	public Document handlePunchOutRequest(ConfigCachePunchoutAriba cc, Element root,String buyerCookie,String postUrl,String addressID,String networkID)
			throws JDOMException {
		String cMethod = "handlePunchOutRequest";
		log.entering(cName, cMethod);

		//Write the record to the db, buyer cookie was passed to read by reference 
		AribaDbRequest aribaRequest = new AribaDbRequest(cc.getAribaCompany(),cc.getAribaWebsiteId(),cc.getJndiName(),cc.getProviderURL(),session,cc.getAribaUser(),buyerCookie);
		//This is a mod I will pass in the postUrl variable here so i do not have to mod the TAI 
		aribaRequest.setPunchoutRequest(postUrl);	
		//String userName = aribaRequest.getUser(networkID); // We get this from the Config file

		String shoppingUrl = cc.getShoppingURL(); //"http://staging.widexpro.com/wps/myportal/home";
		// create new session in web shop here and set URL
		String userID = aribaRequest.getUser(addressID, networkID);
		if (userID == null || userID.trim().isEmpty() || userID.trim().equals("INVALID")) {
			userID = cc.getAribaUser();
		}
		
		postUrl = Base64.encodeBase64URLSafeString(postUrl.getBytes());
		
		shoppingUrl=shoppingUrl+"?source=Ariba&session="+session+"&buyerCookie="+buyerCookie+"&postUrl="+postUrl+"&addressID="+addressID+"&networkID="+networkID+"&user="+userID;
		log.fine("shopping url = "+shoppingUrl);
		Document response = createResponse(getTimestamp(),getPayloadId(),"200","OK");
		response.getRootElement().getChild("Response")
		.addContent(new Element("PunchOutSetupResponse")
		.addContent(new Element("StartPage")
		.addContent(new Element("URL")
		.setText(shoppingUrl))));

		log.exiting(cName, cMethod);
		return response;
	}

	public Document handleOrderRequest (Element root, String addressId, String networkId) throws JDOMException {
		String cMethod = "handleOrderRequest";
		log.entering(cName, cMethod);
		
		ConfigCachePunchoutAriba cc = ConfigCachePunchoutAriba.getInstance();
		Document response = null;
		try {
			log.finest("In the order request try");
			String poNumber = root.getChild("Request").getChild("OrderRequest").getChild("OrderRequestHeader").getAttributeValue("orderID");
			//root.getChild("Request").getChild("OrderRequest").getChild("OrderRequestHeader").getAttributeValue("orderID");
			//String addressID = root.getChild("Request").getChild("OrderRequest").getChild("OrderRequestHeader").getChild("ShipTo").getChild("Address").getAttributeValue("addressID");
			//String networkID = root.getChild("Header").getChild("From").getChild("Credential").getChild("Identity").getTextTrim();
			//Get the BillTo/ShipTo here -- if AddressId is invalid it will return an error
			//AribaDbRequest adbr = new AribaDbRequest(cc.getJndiName(), cc.getProviderURL(), cc.getAribaUser(), this.session);
			String buyerCookie = "";
			String postUrl = "";
			AribaDbRequest adbr = new AribaDbRequest(cc.getAribaCompany(), cc.getAribaWebsiteId(), cc.getJndiName(), cc.getProviderURL(), this.session, cc.getAribaUser(), buyerCookie);
			ArrayList<String> customerInfo = new ArrayList<String>();
			customerInfo = adbr.getCustShipTo(addressId, networkId);

			if(customerInfo.get(0).toString().equalsIgnoreCase("INVALID")) {
				response = createResponse(getTimestamp(),getPayloadId(), "700", "Error - Customer is not currently configured to order from " + cc.getDomain() + ".");
			} else {
				
				//here's where we're going to load the UOM translation matrix (if it exists)
				Map<String, String> uomTranslationMatrix = adbr.getUomTranslationMatrix(networkId);
				
				//we're going to parse the CXML here.
				
				Element orderRequest = root.getChild("Request").getChild("OrderRequest");
				
				List<Content> itemsFromCXML = orderRequest.getChildren("ItemOut");
				
				if (itemsFromCXML.size() == 0)
				{
					log.severe("Error: 400: Bad Request: no items in order request.");
					return createResponse(new PunchoutResponse(400, "Bad Request", "Error: no items in order request."));
				}
				
				AribaOrder order = null;
				boolean newOrder = false;
				Set<Integer> lineNumbers = new HashSet<Integer>();
				int masterLineNumber = 0;//this is only used when the customer does not provide line numbers
				for (Content c : itemsFromCXML)
				{
					Element itemOut = ((Element)c);
					Element itemID = itemOut.getChild("ItemID");
					//String itemDescription = itemOut.getChild("ItemDetail").getChildText("Description");
					int orderNumber = -1;
					try
					{
						orderNumber = Integer.parseInt(itemID.getChild("SupplierPartAuxiliaryID").getText());
					}
					catch (Exception e)
					{
						newOrder=true;
					}
					if (order != null && orderNumber != order.getOrderNumber())
					{
						newOrder = true;
					}
					String itemNumber = itemID.getChild("SupplierPartID").getText().trim();
					String unitOfMeasure = itemOut.getChild("ItemDetail").getChildText("UnitOfMeasure").trim();
					
					//check to see if we need to do a translation
					
					if (uomTranslationMatrix != null && !uomTranslationMatrix.isEmpty())
					{
						String newUom = uomTranslationMatrix.get(unitOfMeasure);//already trimmed
						if (newUom != null && newUom.length()>0)
						{
							//we'll want to log this
							log.logp(Level.FINER, cName, cMethod, "unit of measure changed from '"+unitOfMeasure+"' to '"+newUom+"'.");
							unitOfMeasure = newUom;
						}
						else
						{
							//no translation found. perhaps we should error out, but for now, we're going to just accept it as it was sent and let it fail later.
						}
					}
					
					String itemQuantityString = itemOut.getAttribute("quantity").getValue().trim();
					int itemQuantity = 0;
					
					try
					{
						itemQuantity = new BigDecimal(itemQuantityString).intValueExact();
					}
					catch (ArithmeticException a)
					{
						return createResponse(new PunchoutResponse(400, "Bad Request", "Error: Non-integral quantity ('"+itemQuantityString+"') for itemNumber='"+itemNumber+"'."));
					}
					
					if (itemQuantity < 1)
					{
						log.severe("Error: 400: Bad Request: Error: quantity cannot be less than 1. orderNumber="+orderNumber+", itemNumber="+itemNumber+", quantity="+itemQuantity+".");
						return createResponse(new PunchoutResponse(400, "Bad Request", "Error: quantity cannot be less than 1. orderNumber="+orderNumber+", itemNumber="+itemNumber+", quantity="+itemQuantity+"."));
					}
					
					String itemLineNumberString = null;
					Integer lineNumber = null;
					boolean customerProvidedAtLeastOneLineNumber = false;
					try
					{
						lineNumber = itemOut.getAttribute("lineNumber").getIntValue();
						customerProvidedAtLeastOneLineNumber = true;
					}
					catch (Exception e)
					{
						if (customerProvidedAtLeastOneLineNumber)
						{
							//we only error when at least one but not all lines have numbers
							log.severe("Error: 400: Bad Request: Error parsing line number. orderNumber="+orderNumber+", lineNumber='"+itemLineNumberString+"', itemNumber="+itemNumber+", quantity="+itemQuantity+".");
							e.printStackTrace();
							return createResponse(new PunchoutResponse(400, "Bad Request", "Error parsing lineNumber ('"+itemLineNumberString+"') for itemNumber='"+itemNumber+"'."));
						}
						else
						{
							lineNumber = ++masterLineNumber;
						}
					}
					
					if (lineNumber == null)
					{
						log.severe("Error: 400: Bad Request: Error: missing line number. orderNumber="+orderNumber+", lineNumber='"+lineNumber+"', itemNumber="+itemNumber+", quantity="+itemQuantity+".");
						return createResponse(new PunchoutResponse(400, "Bad Request", "Error parsing lineNumber ('"+itemLineNumberString+"') for itemNumber='"+itemNumber+"'."));
					}
					else
					{
						if (!lineNumbers.add(lineNumber))
						{
							//we have a duplicate!
							log.severe("Error: 400: Bad Request: Error: duplicate line number. orderNumber="+orderNumber+", lineNumber='"+lineNumber+"', itemNumber="+itemNumber+", quantity="+itemQuantity+".");
							return createResponse(new PunchoutResponse(400, "Bad Request", "Error parsing lineNumber ('"+itemLineNumberString+"') for itemNumber='"+itemNumber+"'."));
						}
					}
					
					log.finer("cxml item {orderNumber="+orderNumber+", lineNumber="+lineNumber+", itemNumber='"+itemNumber+"', itemUom='"+unitOfMeasure+"', itemQuantity="+itemQuantity+"}");
					
					if (order == null)
					{
						order = new AribaOrder(poNumber, orderNumber);
					}
					
					//TODO: handle duplicates
					order.getItemDetails().add(new AribaOrderDetails(lineNumber, itemNumber, unitOfMeasure, itemQuantity));
				}
				
				//now we have the order from CXML
				
				Collections.sort(order.getItemDetails(), new AribaOrderDetailsLineNumberComparator());
				
				//TODO: remove test code
				for (AribaOrderDetails details : order.getItemDetails())
				{
					log.logp(Level.FINEST, cName, cMethod, "cxml: "+details.toString());
				}
				
				//I'm going to check the original s2k order - because if we can just pass this through, it will be faster and easier.
				if (!newOrder && adbr.itemsMatchExistingS2kOrder(order))
				{
					//this order matches the original
					//NOTE: this will also update the customer/ship-to
					if (!adbr.completePunchOutOrder(order, networkId, addressId))
					{
						log.severe("Error: 500: Internal Server Error: Something went wrong processing order requests. Please contact "+cc.getDomain()+" for technical assistance with this order.");
						return createResponse(new PunchoutResponse(500, "Internal Server Error", "Please contact "+cc.getDomain()+" for technical assistance with this order ("+order.getOrderNumber()+")."));
					}
					else
					{
						//we're done with this PO
						return response = createResponse(new PunchoutResponse(200, "Success"));
					}
				}
				
				//if we get to here, we know we have to make a new order.
				
				//get the customer info
				adbr.setCustomerAndShipToInfoForOrder(order, networkId, addressId);
				
				//quick order does this first, so we will, too.
				List<Item> orderItems = adbr.getItemInfo(order, this.session);
				
				for (Item item : orderItems)
				{
					if(item.getDescription1().equalsIgnoreCase("ITEM NOT FOUND"))
					{
						//we're going to handle this differently than the portal does. if we don't find an item, we're going to reject the whole order - not just that item
						return createResponse(new PunchoutResponse(400, "Bad Request", "Error: Invalid Item Number '"+item.getItemNumber()+"'."));
					}
					if (item.getItemType().equals("S") || item.getIsActive().equalsIgnoreCase("I"))
					{
						//this is an error in quick order
						return createResponse(new PunchoutResponse(400, "Bad Request", "Error: Invalid Item. Item number = '"+item.getItemNumber()+", 'type = '"+item.getItemType()+"'"));
					}
				}
				
				int sequence = 1;
				
				//get keys and stuff
				order.setOrderNumber(adbr.getOrderNumber());
				order.setCartKey(adbr.getCartId());
				
				log.logp(Level.FINE, cName, cMethod, "cartKey="+order.getCartKey());
				
				//let's make a vebsess record
				adbr.createVEBSESSRecord(order, "I", "INITIAL", "",postUrl, buyerCookie, networkId, addressId, sequence);
				
				for (Item item : orderItems)
				{
					// Do not process this item if the item description reflects not found
					if(!item.getDescription1().equalsIgnoreCase("ITEM NOT FOUND"))
					{
						// Only perform the rest if the quantity is greater than 0
						if(Integer.parseInt(item.getQuantityForQuickOrder()) > 0)
						{
							//find the uom
							StringBuilder availableUoms = new StringBuilder();
							availableUoms.append("{");
							UnitOfMeasure unitOfMeasure = null;
							for (UnitOfMeasure uom : item.getUomList())
							{
								availableUoms.append("'");
								availableUoms.append(uom.getUnitOfMeasure());
								availableUoms.append("', ");
								if (uom.getUnitOfMeasure().equals(item.getCxmlUom()))
								{
									unitOfMeasure = uom;
									break;
								}
							}
							int lastCommaIndex = availableUoms.lastIndexOf(", ");
							if (lastCommaIndex > -1)
							{
								availableUoms.setLength(lastCommaIndex);
							}
							availableUoms.append("}");
							
							//we need to check this for null - if null, the unit of measure doesn't exist
							if (unitOfMeasure == null)
							{
								//send out a rejection
								return createResponse(new PunchoutResponse(400, "Bad Request", "Error: Invalid Unit of Measure specified for item number = '"+item.getItemNumber()+". UOM = '"+item.getCxmlUom()+"', available UOMs "+(new String(availableUoms))));
							}
							// Put the new quantity,price and UOM back into the item object so I can add it to the cart
							item.setUnitMeasure(unitOfMeasure.getUnitOfMeasure());
							item.setCartUOM(unitOfMeasure.getUnitOfMeasure());
							item.setQtyInCart(Integer.parseInt(item.getQuantityForQuickOrder()));
							// Call pricing string procedure here to get all my pricing info
							adbr.getWebPrice(item,order.getCustomer_nbr(),order.getShipto_nbr(),session,order.getLocation());
							
							// Insert the item
							adbr.insertItem(item, order.getCartKey(), order.getCustomer_nbr(),
								order.getShipto_nbr(), "Y",//s2k pricing is Y
								order.getLocation());
						}
					}
				}
				
				adbr.insertShipTo(order);
				//okay. so now we have the items in VEBCART and a VEBSESS record.

				//all we have to do is complete the checkout.
				String requestedShipDate = new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());
				adbr.updateVEBSESSRecordBeforeComplete(order, "0", requestedShipDate, sequence);
				
				int s2kOrder = adbr.getOrderNumberS2K();
				adbr.insertS2KOrderNumber(order.getCartKey(), s2kOrder);
				
				List<ShippingOption> shipVia = adbr.getShippingOptions(order.getCustomer_nbr(), order.getShipto_nbr(), order.getLocation(), "", "", order.getCartKey(), order.getShipVia(), "N", "N", "Y", "");
				for (ShippingOption so : shipVia)
				{
					if (so.getShipViaCode().equals(order.getShipVia()))
					{
						adbr.updateShippingInfo(order.getCartKey(), so);
						break;
					}
				}
				
				// Complete the order. This sets completion flag on line items to 'N'
				adbr.completeOrder(order.getCartKey(), true, false, sequence);
				
				return response = createResponse(new PunchoutResponse(200, "Success"));
			}
		} catch(Exception e){
			log.severe("Exception from Order Request = "+e.getMessage() + "\n" + ExceptionUtils.getStackTrace(e));
			response = createResponse(getTimestamp(),getPayloadId(), "600", "Error - The " + cc.getDomain() + " system is currently unavailable, please try again in a few minutes. ");
		}	

		log.exiting(cName, cMethod);
		return response;
	}

	public Document handleProfileRequest (Element root) throws JDOMException {
		ConfigCachePunchoutAriba cc = ConfigCachePunchoutAriba.getInstance();

		Document response = createResponse(getTimestamp(),getPayloadId(),"200","OK");
		Element respRoot = response.getRootElement().getChild("Response");
		respRoot.addContent(new Element("ProfileResponse").setAttribute("effectiveDate",getTimestamp())
				.addContent(new Element("Transaction").setAttribute("requestName","PunchOutSetupRequest")
						.addContent(new Element("URL").setText(SqlUtil.magicPrintSQL(PUNCHOUT_URL, cc.getDomain())))
						.addContent(new Element("Option").setAttribute("name","operationAllowed").setText(OPERATION_ALLOWED))));
		if (cc.allowsOrders()) {
			respRoot.addContent(new Element("Transaction").setAttribute("requestName","OrderRequest")
					.addContent(new Element("URL").setText(ORDER_URL))
					.addContent(new Element("Option").setAttribute("name","attachments").setText(ORDER_ATTACHMENTS))
					.addContent(new Element("Option").setAttribute("name","changes").setText(ORDER_CHANGES))
					);
		}
		return response;
	}



	// Creates a generic response document
	public Document createResponse (String sTimestamp, String payloadId, String statusCode, String statusText)
			throws JDOMException {
		String cMethod = "createResponse";
		log.entering(cName, cMethod);
		Document response = new Document(new Element("cXML"))
		.setDocType(new DocType("cXML", "http://xml.cXML.org/schemas/cXML/1.2.028/cXML.dtd"));

		response.getRootElement().setAttribute("version","1.2.006")
		//.setAttribute("xml:lang","en-US")
		.setAttribute("timestamp",sTimestamp)
		.setAttribute("payloadID",payloadId)
		.addContent(new Element("Response")
		.addContent(new Element("Status")
		.setAttribute("code",statusCode)
		.setAttribute("text",statusText)));

		log.exiting(cName, cMethod);
		return response;
	}

	public Document createResponse (String sTimestamp, String payloadId, String statusCode, String statusText, String statusMessage)
			throws JDOMException {
		String cMethod = "createResponse";
		log.entering(cName, cMethod);
		Document response = new Document(new Element("cXML"))
		.setDocType(new DocType("cXML", "http://xml.cXML.org/schemas/cXML/1.2.028/cXML.dtd"));

		response.getRootElement().setAttribute("version","1.2.006")
		//.setAttribute("xml:lang","en-US")
		.setAttribute("timestamp",sTimestamp)
		.setAttribute("payloadID",payloadId)
		.addContent(new Element("Response")
		.addContent(new Element("Status")
		.setAttribute("code",statusCode)
		.setAttribute("text",statusText).setText(statusMessage)));

		log.exiting(cName, cMethod);
		return response;
	}
	
	public boolean lookupBuyer (String domain, String id) {
		// here you would check if the buyer is known
		// e.g. if ((domain == "NetworkId") && (id == "AN01000002792-T")) return true;
		// note that the From element might contain multiple credentials
		return true;
	}

	// Timestamp
	// The date and time the message was sent, in ISO 8601 format.
	// This value should not change for retry attempts.
	// The format is YYYY-MM-DDThh:mm:ss-hh:mm (for example, 1997-07-16T19:20:30+01:00).
	//
	// Hard coded for now. Should use current date/time in below format

	public String getTimestamp () {
		Date date = new Date();
		return date.toString();
	}

	// PayloadId
	// A unique number with respect to space and time, used for
	// logging purposes to identify documents that might have been
	// lost or had problems. This value should not change for retry attempts.
	// The recommended implementation is: datetime.process id.random number@hostname
	public String getPayloadId () {
		ConfigCachePunchoutAriba cc = ConfigCachePunchoutAriba.getInstance();
		long pseudoRandom = System.nanoTime(); // Close enough for government work...

		return getTimestamp()+"." + Long.toString(pseudoRandom).substring(2,6) + "." + Long.toOctalString(pseudoRandom).substring(2,6)+ "@" + cc.getDomain();
	}

	// Saves orders to file
	public void saveOrder(String s) {
		String logfile = "/logger/aribalog.txt";
		try {
			PrintWriter fOut = new PrintWriter(new FileWriter(logfile, true));
			fOut.write(s);
			fOut.close();
		}
		catch (IOException e) {
			log.severe("File I/O error:" + e.getMessage() + "\n" + ExceptionUtils.getStackTrace(e));
		}
	}

	private ArrayList<OrderDetail> getOrderDetails(Element root, int company, String orderId, int orderVersion) {
		String cMethod = "getOrderDetails";
		log.entering(cName, cMethod);
		ArrayList<OrderDetail> orderDetails = new ArrayList<OrderDetail>();

		//Get the Line Items here
		List<Content> orderLines = (List<Content>) (root.getChild("Request").getChild("OrderRequest").getChildren("ItemOut"));
		Iterator<Content> i = orderLines.iterator();

		while (i.hasNext()) {
			Element e = (Element)i.next();
			OrderDetail orderDetail = new OrderDetail();
			orderDetail.setLineNumber(Integer.parseInt(e.getAttributeValue("lineNumber")));
			orderDetail.setQuantity(Integer.parseInt(e.getAttributeValue("quantity")));
			orderDetail.setUnitPrice(Double.parseDouble(e.getChild("ItemDetail").getChild("UnitPrice").getChild("Money").getValue()));
			orderDetail.setUnitOfMeasure(e.getChild("ItemDetail").getChild("UnitOfMeasure").getValue());
			orderDetail.setItemNumber(e.getChild("ItemID").getChild("SupplierPartID").getValue());
			orderDetail.setOrderId(orderId);
			orderDetail.setOrderVersion(orderVersion);
			orderDetail.setCompany(company);

			orderDetails.add(orderDetail);
		}
		log.exiting(cName, cMethod);
		return orderDetails;
	}


	private OrderHeader populateOrderHeader(Element root, ArrayList<String> customerInfo){
		Element orderRequestHeader = root.getChild("Request").getChild("OrderRequest").getChild("OrderRequestHeader");
		OrderHeader orderHeader = new OrderHeader();
		orderHeader.setCustNumber(customerInfo.get(0).toString());
		orderHeader.setShipNumber(customerInfo.get(1).toString());
		orderHeader.setCompany(Integer.parseInt(customerInfo.get(2).toString()));

		orderHeader.setNetworkId(root.getChild("Header").getChild("From").getChild("Credential").getChild("Identity").getTextTrim());
		orderHeader.setOrderId(orderRequestHeader.getAttributeValue("orderID"));
		//PO Date comes in as follows...  2014-02-06T05:00+00
		String poDate = orderRequestHeader.getAttributeValue("orderDate").substring(0, 11);
		orderHeader.setPoDate(poDate);
		orderHeader.setOrderVersion(Integer.parseInt(orderRequestHeader.getAttributeValue("orderVersion")));
		orderHeader.setOrderTotal(Double.parseDouble(orderRequestHeader.getChild("Total").getChild("Money").getTextTrim()));
		orderHeader.setShipAddressId(orderRequestHeader.getChild("ShipTo").getChild("Address").getAttributeValue("addressID"));
		orderHeader.setShipDeliverTo(orderRequestHeader.getChild("ShipTo").getChild("Address").getChild("PostalAddress").getChild("DeliverTo").getTextTrim());
		orderHeader.setShipStreet(orderRequestHeader.getChild("ShipTo").getChild("Address").getChild("PostalAddress").getChild("Street").getTextTrim());
		orderHeader.setShipCity(orderRequestHeader.getChild("ShipTo").getChild("Address").getChild("PostalAddress").getChild("City").getTextTrim());
		orderHeader.setShipState(orderRequestHeader.getChild("ShipTo").getChild("Address").getChild("PostalAddress").getChild("State").getTextTrim());
		orderHeader.setShipZip(orderRequestHeader.getChild("ShipTo").getChild("Address").getChild("PostalAddress").getChild("PostalCode").getTextTrim());
		orderHeader.setShipCountry(orderRequestHeader.getChild("ShipTo").getChild("Address").getChild("PostalAddress").getChild("Country").getAttributeValue("isoCountryCode"));
		orderHeader.setShipEmail(orderRequestHeader.getChild("ShipTo").getChild("Address").getChild("Email").getAttributeValue("name"));

		orderHeader.setBillAddressId(orderRequestHeader.getChild("BillTo").getChild("Address").getAttributeValue("addressID"));
		orderHeader.setBillDeliverTo(orderRequestHeader.getChild("BillTo").getChild("Address").getChild("PostalAddress").getChild("DeliverTo").getTextTrim());
		orderHeader.setBillStreet(orderRequestHeader.getChild("BillTo").getChild("Address").getChild("PostalAddress").getChild("Street").getTextTrim());
		orderHeader.setBillCity(orderRequestHeader.getChild("BillTo").getChild("Address").getChild("PostalAddress").getChild("City").getTextTrim());
		orderHeader.setBillState(orderRequestHeader.getChild("BillTo").getChild("Address").getChild("PostalAddress").getChild("State").getTextTrim());
		orderHeader.setBillZip(orderRequestHeader.getChild("BillTo").getChild("Address").getChild("PostalAddress").getChild("PostalCode").getTextTrim());
		orderHeader.setBillCountry(orderRequestHeader.getChild("BillTo").getChild("Address").getChild("PostalAddress").getChild("Country").getAttributeValue("isoCountryCode"));
		orderHeader.setBillEmail(orderRequestHeader.getChild("BillTo").getChild("Address").getChild("Email").getAttributeValue("name"));

		orderHeader.setContactName(orderRequestHeader.getChild("Contact").getChild("Name").getTextTrim());

		return orderHeader;
	}
	
	private Document handleInvoiceRequest(Element root)
	{
		Document response = null;
		try
		{
			response = createResponse(getTimestamp(), getPayloadId(), "201", "Accepted", "Acknowledged");
		}
		catch (JDOMException e)
		{
			e.printStackTrace();
		}
		return response;
	}
	
	public void setResponse(PunchoutResponse punchoutResponse)
	{
		this.response = punchoutResponse;
	}
	
	public Document createResponse (PunchoutResponse punchoutResponse)
			throws JDOMException {
		String cMethod = "createResponse";
		log.entering(cName, cMethod);
		
		Document response = new Document(new Element("cXML"))
		.setDocType(new DocType("cXML", "http://xml.cXML.org/schemas/cXML/1.2.028/cXML.dtd"));
		
		response.getRootElement().setAttribute("version","1.2.006")
		//.setAttribute("xml:lang","en-US")
		.setAttribute("timestamp",punchoutResponse.getTimestamp())
		.setAttribute("payloadID",punchoutResponse.getPayloadID())
		.addContent(new Element("Response")
		.addContent(new Element("Status")
		.setAttribute("code",Integer.toString(punchoutResponse.getCode()))
		.setAttribute("text",punchoutResponse.getText()).setText(punchoutResponse.getMessage())));
		
		log.exiting(cName, cMethod, punchoutResponse.getCode()+": "+punchoutResponse.getMessage());
		return response;
	}
}