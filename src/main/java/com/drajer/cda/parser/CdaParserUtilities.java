package com.drajer.cda.parser;

import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class CdaParserUtilities {

  private static final Logger logger = LoggerFactory.getLogger(CdaParserUtilities.class);

  public static ArrayList<CdaIi> readTemplateIdList(NodeList templateIDNodeList) {
    ArrayList<CdaIi> templateList = null;
    if (!isNodeListEmpty(templateIDNodeList)) {
      templateList = new ArrayList<>();
    }

    Element templateElement;
    for (int i = 0; i < templateIDNodeList.getLength(); i++) {
      templateElement = (Element) templateIDNodeList.item(i);
      templateList.add(readTemplateId(templateElement));
    }
    return templateList;
  }

  public static CdaIi readTemplateId(Element templateElement) {
    CdaIi templateID = null;

    if (templateElement != null) {
      templateID = new CdaIi();
      if (!isEmpty(templateElement.getAttribute("root"))) {
        templateID.setRootValue(templateElement.getAttribute("root"));
        logger.info(" Root Value = {} ", templateID.getRootValue());
      }
      if (!isEmpty(templateElement.getAttribute("extension"))) {
        templateID.setExtValue(templateElement.getAttribute("extension"));
        logger.info(" Root Value = {} ", templateID.getExtValue());
      }
    }
    return templateID;
  }

  public static boolean isEmpty(final String str) {
    return str == null || str.trim().length() == 0;
  }

  public static boolean isNodeListEmpty(final NodeList nodeList) {
    return nodeList == null || nodeList.getLength() == 0;
  }
}