/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentassurance.helpers

object ChangeDesiDetailsPayloads {

  // this is actual output taken from agent-services-account-frontend
  val specialChars: String =
    """<!DOCTYPE html PUBLIC "-//OPENHTMLTOPDF//DOC XHTML Character Entities Only 1.0//EN" "">
      |<html>
      |    <head>
      |        <title>Request to amend contact details</title>
      |        <style>
      |                body{font-family:Arial,sans-serif;font-size: 16px; margin:50px;}
      |                dl{border-bottom: 1px solid #bfc1c3;}
      |                dt{font-weight: bold;}
      |                dt,dd{margin:0; width: 100%; display:block; text-align:left; padding-left:0;padding-bottom:15px;}
      |        </style>
      |    </head>
      |    <body>
      |
      |
      |
      |<h1 class="govuk-heading-l ">Request to amend contact details</h1>
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |<h2 class ="govuk-heading-m " >Business details</h2>
      |
      |
      |            <p>Unique Taxpayer Reference: 1234567890</p>
      |
      |        <p>Agent reference number: XARN0077777</p>
      |
      |
      |
      |
      |<h2 class ="govuk-heading-m " >Existing contact details</h2>
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |  <dl class="govuk-summary-list">
      |
      |    <div class="govuk-summary-list__row">
      |      <dt class="govuk-summary-list__key">
      |        Business name shown to clients
      |      </dt>
      |      <dd class="govuk-summary-list__value">
      |        GREENDALE ACCOUNTING &amp;&lt;&gt;&quot;&#x27; TAILORING LTD
      |      </dd>
      |
      |    </div>
      |
      |    <div class="govuk-summary-list__row">
      |      <dt class="govuk-summary-list__key">
      |        Address for agent services account
      |      </dt>
      |      <dd class="govuk-summary-list__value">
      |        SPACE &amp;&lt;&gt;&quot;&apos; TIME<br />1 GREENDALE &amp;&lt;&gt;&quot;&apos; ROAD<br />GREEN &amp;&lt;&gt;&quot;&apos; DALE<br />PLANET &amp;&lt;&gt;&quot;&apos; EARTH<br />QL4 5EE<br />GB
      |      </dd>
      |
      |    </div>
      |
      |  </dl>
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |<h2 class ="govuk-heading-m " >New contact details</h2>
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |  <dl class="govuk-summary-list">
      |
      |    <div class="govuk-summary-list__row">
      |      <dt class="govuk-summary-list__key">
      |        Business name shown to clients
      |      </dt>
      |      <dd class="govuk-summary-list__value">
      |        Greendale Accounting &amp;&lt;&gt;&quot;&#x27; Tailoring Ltd
      |      </dd>
      |
      |    </div>
      |
      |    <div class="govuk-summary-list__row">
      |      <dt class="govuk-summary-list__key">
      |        Address for agent services account
      |      </dt>
      |      <dd class="govuk-summary-list__value">
      |        2 &amp;&lt;&gt;&quot;&apos; Lane<br />Green &amp;&lt;&gt;&quot;&apos; dale<br />APPLES &amp;&lt;&gt;&quot;&apos;<br />&amp;&lt;&gt;&quot;&apos; LEMONS<br />QL1 5RR<br />GB
      |      </dd>
      |
      |    </div>
      |
      |  </dl>
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |<h2 class ="govuk-heading-m " >Other services to be amended with same details</h2>
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |  <dl class="govuk-summary-list">
      |
      |    <div class="govuk-summary-list__row">
      |      <dt class="govuk-summary-list__key">
      |        Apply changes to Self Assessment
      |      </dt>
      |      <dd class="govuk-summary-list__value">
      |        Yes
      |      </dd>
      |
      |    </div>
      |
      |    <div class="govuk-summary-list__row">
      |      <dt class="govuk-summary-list__key">
      |        Self Assessment agent code
      |      </dt>
      |      <dd class="govuk-summary-list__value">
      |        1234HB
      |      </dd>
      |
      |    </div>
      |
      |    <div class="govuk-summary-list__row">
      |      <dt class="govuk-summary-list__key">
      |        Apply changes to Corporation Tax
      |      </dt>
      |      <dd class="govuk-summary-list__value">
      |        Yes
      |      </dd>
      |
      |    </div>
      |
      |    <div class="govuk-summary-list__row">
      |      <dt class="govuk-summary-list__key">
      |        Corporation Tax agent code
      |      </dt>
      |      <dd class="govuk-summary-list__value">
      |        F1234L
      |      </dd>
      |
      |    </div>
      |
      |  </dl>
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |<h2 class ="govuk-heading-m " >User’s contact details</h2>
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |  <dl class="govuk-summary-list">
      |
      |    <div class="govuk-summary-list__row">
      |      <dt class="govuk-summary-list__key">
      |        Full name
      |      </dt>
      |      <dd class="govuk-summary-list__value">
      |        Troy Barnes
      |      </dd>
      |
      |    </div>
      |
      |    <div class="govuk-summary-list__row">
      |      <dt class="govuk-summary-list__key">
      |        Telephone number
      |      </dt>
      |      <dd class="govuk-summary-list__value">
      |        01726354656
      |      </dd>
      |
      |    </div>
      |
      |  </dl>
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |
      |    </body>
      |</html>
      |""".stripMargin

}
