package cz.bliksoft.javautils.ws;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import cz.bliksoft.javautils.logging.LogUtils;
import cz.bliksoft.javautils.threads.MessageInterceptWorker;
import cz.bliksoft.javautils.xml.XmlUtils;

public class SignHandler implements SOAPHandler<SOAPMessageContext> {
	private Logger log = Logger.getLogger(SignHandler.class.getName());

	private String _name;

	public enum SignType {
		ROOT, FIRSTCHILD
	}

	SignType _signType = SignType.ROOT;

	public SignHandler(SignType signType, String name) {
		_signType = signType;
		_name = name;
	}

	@Override
	public boolean handleMessage(final SOAPMessageContext msgCtx) {
		// Indicator telling us which direction this message is going in
		final Boolean outInd = (Boolean) msgCtx.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

		// Handler must process only outbound messages
		if (outInd.booleanValue()) {
			log.fine("SignHandler handle outbound message");
			try {
				if (Thread.currentThread() instanceof MessageInterceptWorker) {
					MessageInterceptWorker worker = (MessageInterceptWorker) Thread.currentThread();

					final TransformerFactory transformerFactory = TransformerFactory.newInstance();
					final Transformer transformer = transformerFactory.newTransformer();

					// Format it
					transformer.setOutputProperty(OutputKeys.INDENT, "yes");
					transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

					Document body = msgCtx.getMessage().getSOAPBody().extractContentAsDocument();

					Node vstupNode = null;

					if (_signType == SignType.ROOT) {
						vstupNode = body.getDocumentElement();
						worker.setPreparedRecord(XmlUtils.getStringFromNode(vstupNode));
					} else if (_signType == SignType.FIRSTCHILD) {
						vstupNode = body.getDocumentElement().getFirstChild();
						worker.setPreparedRecord(XmlUtils.getStringFromNode(vstupNode.getLastChild()));
					}

					if (worker.signedLock.tryAcquire(5, TimeUnit.MINUTES)) {
						if (worker.getModifiedRecord() == null) {
							log.info("skipping signature insertion (no signed record provided)");
						} else if (worker.getModifiedRecord().equals(worker.getPreparedRecord())) {
							log.info("skipping signature insertion (request not changed)");
						} else {
							log.info("Replacing body with signed content");
							Node node = null;
							Node signedRecord = null;

							try {
								node = XmlUtils.convertStringToNode(worker.getModifiedRecord());
								signedRecord = body.importNode(node, true);
							} catch (Exception e) {
								log.severe("Unable to parse signed record XML! " + e.getMessage());
								return false;
							}

							try {
								if (_signType == SignType.ROOT) {
									body.replaceChild(signedRecord, vstupNode);
								} else if (_signType == SignType.FIRSTCHILD) {
									vstupNode.replaceChild(signedRecord, vstupNode.getLastChild());
								}
							} catch (Exception e) {
								log.severe("Unable to replace the old node! " + e.getMessage());
								return false;
							}

						}
						msgCtx.getMessage().getSOAPBody().addDocument(body);

						File logFile = LogUtils.getFile("SOAP{" + this._name + "}_req_signed", "xml");

						if (logFile == null)
							return true;

						try (FileOutputStream fos = new FileOutputStream(logFile)) {
							msgCtx.getMessage().writeTo(fos);
						} catch (Exception e) {
							log.severe("Error logging signed SOAP message.");
						}

						return true;
					} else {
						throw new RuntimeException("Request signature timeout.");
					}

				} else {
					return true;
				}
			} catch (final Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean handleFault(SOAPMessageContext context) {
		return true;
	}

	@Override
	public void close(MessageContext context) {
	}

	@Override
	public Set<QName> getHeaders() {
		return null;
	}

}
