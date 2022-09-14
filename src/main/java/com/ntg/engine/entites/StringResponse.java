package com.ntg.engine.entites;

import javax.xml.bind.annotation.XmlElement;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER. Copyright 1997-2016
 * NTG Clarity and/or its affiliates. All rights reserved. NTG CLARITY is a
 * leader in delivering network, telecom, IT and infrastructure solutions to
 * network service providers and medium and large enterprises.
 * www.ntgclarity.com The contents of this file are subject to the terms of "NTG
 * Clarity License". You must not use this file except in compliance with the
 * License. You can obtain a copy of the License at http://www.ntgclarity.com/
 * See the License for the specific language governing permissions and
 * limitations under the License. Contributor(s): The Initial Developer of the
 * Original Software is NTG Clarity . , Inc. Copyright 1997-2016 NTG Clarity.
 * All Rights Reserved. CLASS NAME
 * <h4>Description</h4>
 * <h4>Notes</h4>
 * <h4>References</h4>
 * 
 * @author: VoWifiDeveloper <A HREF="mailto:stagedev@ntgclarity.com">VoWIFI
 *          Development Team</A>
 * @version Revision: 1.0 Date: 01/05/17 10:22 AM
 * @see [String]
 * @see [URL]
 * @see [Class name#method name]
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY)
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)

public class StringResponse extends BaseResponse {

	public StringResponse() {
	}

	public StringResponse(String re) {
		this.returnValue = re;
	}

	public StringResponse(String[] re) {
		if (re != null && re.length > 1) {
			returnValue = re[0];
			for (int i = 1; i < re.length; i++) {
				returnValue += "," + re[i];
			}
		} else if (re.length == 1) {
			returnValue = re[0];
		}
	}

	@JsonInclude(Include.NON_EMPTY)
	@XmlElement
	public String returnValue;

}
