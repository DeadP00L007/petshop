<%--

       Copyright 2010-2025 the original author or authors.

       Licensed under the Apache License, Version 2.0 (the "License");
       you may not use this file except in compliance with the License.
       You may obtain a copy of the License at

          https://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing, software
       distributed under the License is distributed on an "AS IS" BASIS,
       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
       See the License for the specific language governing permissions and
       limitations under the License.

--%>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %>

<h2>Order Payment</h2>

<stripes:form action="/actions/Order.action" method="post">
  <stripes:hidden name="order.orderId"/>

  Card Type:
  <stripes:select name="order.cardType">
    <stripes:options items="${actionBean.creditCardTypes}"/>
  </stripes:select><br/>

  Card Number:
  <stripes:text name="order.creditCard"/><br/>

  Expiry:
  <stripes:text name="order.expiryDate"/><br/>

  <stripes:submit name="submitOrder" value="Continue"/>
</stripes:form>
