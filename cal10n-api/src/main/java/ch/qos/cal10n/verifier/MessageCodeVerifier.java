/*
 * Copyright (c) 2009 QOS.ch All rights reserved.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS  IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package ch.qos.cal10n.verifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import ch.qos.cal10n.util.AnnotationExtractor;
import ch.qos.cal10n.verifier.Cal10nError.ErrorType;

/**
 * Given an enum class, verify that the resource bundles corresponding to a
 * given locale contains the correct codes.
 * 
 * @author Ceki Gulcu
 */
public class MessageCodeVerifier implements IMessageCodeVerifier {

  Class<? extends Enum<?>> enumType;
  String enumTypeAsStr;
  
  public MessageCodeVerifier(Class<? extends Enum<?>> enumClass) {
    this.enumType = enumClass;
    this.enumTypeAsStr = enumClass.getName();
  }

  @SuppressWarnings("unchecked")
  public MessageCodeVerifier(String enumTypeAsStr) {
    this.enumTypeAsStr = enumTypeAsStr;
    String errMsg = "Failed to find enum class [" + enumTypeAsStr + "]";
    try {
      this.enumType = (Class<? extends Enum<?>>) Class.forName(enumTypeAsStr);
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException(errMsg, e);
    } catch (NoClassDefFoundError e) {
      throw new IllegalStateException(errMsg, e);
    }
  }

  
  /* (non-Javadoc)
   * @see ch.qos.cai10n.verifier.IIMessageCodeVerifier#getEnumType()
   */
  public Class<? extends Enum<?>> getEnumType() {
    return enumType;
  }

  /* (non-Javadoc)
   * @see ch.qos.cal10n.verifier.IIMessageCodeVerifier#getEnumTypeAsStr()
   */
  public String getEnumTypeAsStr() {
    return enumTypeAsStr;
  }

  /* (non-Javadoc)
   * @see ch.qos.cal10n.verifier.IIMessageCodeVerifier#verify(java.util.Locale)
   */
  public List<Cal10nError> verify(Locale locale) {
    List<Cal10nError> errorList = new ArrayList<Cal10nError>();

    String resouceBundleName = AnnotationExtractor
        .getResourceBundleName(enumType);

    if (resouceBundleName == null) {
      errorList.add(new Cal10nError(ErrorType.MISSING_RBN_ANNOTATION, "",
          enumType, locale, ""));
      // no point in continuing
      return errorList;
    }

    ResourceBundle rb = ResourceBundle.getBundle(resouceBundleName, locale);

    ErrorFactory errorFactory = new ErrorFactory(enumType, locale,
        resouceBundleName);

    if (rb == null) {
      errorList.add(errorFactory.buildError(ErrorType.FAILED_TO_FIND_RB, ""));
    }

    Set<String> rbKeySet = rb.keySet();
    if (rbKeySet.size() == 0) {
      errorList.add(errorFactory.buildError(ErrorType.EMPTY_RB, ""));
    }

    Enum<?>[] enumArray = enumType.getEnumConstants();
    if (enumArray == null || enumArray.length == 0) {
      errorList.add(errorFactory.buildError(ErrorType.EMPTY_ENUM, ""));
    }

    if (errorList.size() != 0) {
      return errorList;
    }

    for (Enum<?> e : enumArray) {
      String enumKey = e.toString();
      if (rbKeySet.contains(enumKey)) {
        rbKeySet.remove(enumKey);
      } else {
        errorList.add(errorFactory.buildError(ErrorType.ABSENT_IN_RB, enumKey));
      }
    }

    for (String rbKey : rbKeySet) {
      errorList.add(errorFactory.buildError(ErrorType.ABSENT_IN_ENUM, rbKey));
    }
    return errorList;
  }

  /* (non-Javadoc)
   * @see ch.qos.cal10n.verifier.IIMessageCodeVerifier#typeIsolatedVerify(java.util.Locale)
   */
  public List<String> typeIsolatedVerify(Locale locale) {
    List<Cal10nError> errorList = verify(locale);
    List<String> strList = new ArrayList<String>();
    for (Cal10nError error : errorList) {
      strList.add(error.toString());
    }
    return strList;
  }

  /***
   * Verify all declared locales in one step.
   */
  public List<Cal10nError> verifyAllLocales() {
    List<Cal10nError> errorList = new ArrayList<Cal10nError>();
    
    String[] localeNameArray = getLocaleNames();
    
    if (localeNameArray == null || localeNameArray.length == 0) {
      String errMsg = "Missing @LocaleNames annotation in enum type ["
          + enumTypeAsStr + "]";
      throw new IllegalStateException(errMsg);
    }
    for (String localeName : localeNameArray) {
      Locale locale = new Locale(localeName);
      List<Cal10nError> tmpList = verify(locale);
      errorList.addAll(tmpList);
    }
    
    return errorList;
  }

  
  /* (non-Javadoc)
   * @see ch.qos.cal10n.verifier.IIMessageCodeVerifier#getLocaleNames()
   */
  public String[] getLocaleNames() {
    String[] localeNameArray = AnnotationExtractor.getLocaleNames(enumType);
    return localeNameArray;
  }
  
  public String getResourceBundleName() {
    String rbName = AnnotationExtractor.getResourceBundleName(enumType);
    return rbName;
  }

  

}
