/*
 *    Copyright 2010-2025 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.mybatis.jpetstore.web.actions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpSession;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.SessionScope;
import net.sourceforge.stripes.integration.spring.SpringBean;

import org.mybatis.jpetstore.domain.Order;
import org.mybatis.jpetstore.service.OrderService;

@SessionScope
public class OrderActionBean extends AbstractActionBean {

  private static final long serialVersionUID = 1L;

  private static final String NEW_ORDER = "/WEB-INF/jsp/order/orderForm.jsp";
  private static final String CONFIRM = "/WEB-INF/jsp/order/confirmation.jsp";
  private static final String VIEW = "/WEB-INF/jsp/order/ViewOrder.jsp";
  private static final String ERROR = "/WEB-INF/jsp/common/Error.jsp";

  private static final List<String> CARD_TYPES = Collections
      .unmodifiableList(Arrays.asList("Visa", "MasterCard", "American Express"));

  @SpringBean
  private transient OrderService orderService;

  private Order order = new Order();
  private boolean confirmed;

  /* ================= DEFAULT ================= */

  @DefaultHandler
  public Resolution newOrderForm() {
    HttpSession session = context.getRequest().getSession();

    AccountActionBean account = (AccountActionBean) session.getAttribute("/actions/Account.action");
    CartActionBean cart = (CartActionBean) session.getAttribute("/actions/Cart.action");

    if (account == null || !account.isAuthenticated()) {
      setMessage("Please sign in before checkout.");
      return new ForwardResolution(AccountActionBean.class);
    }

    // Fixed: check if cart has zero items
    if (cart == null || cart.getCart().getNumberOfItems() == 0) {
      setMessage("Your cart is empty.");
      return new ForwardResolution(ERROR);
    }

    order.initOrder(account.getAccount(), cart.getCart());
    return new ForwardResolution(NEW_ORDER);
  }

  /* ================= SUBMIT ================= */

  public Resolution submitOrder() {
    HttpSession session = context.getRequest().getSession();

    AccountActionBean account = (AccountActionBean) session.getAttribute("/actions/Account.action");
    CartActionBean cart = (CartActionBean) session.getAttribute("/actions/Cart.action");

    if (account == null || cart == null) {
      setMessage("Session expired. Please login again.");
      return new ForwardResolution(ERROR);
    }

    if (!confirmed) {
      return new ForwardResolution(CONFIRM);
    }

    orderService.insertOrder(order);
    cart.clear();

    setMessage("Order placed successfully.");
    return new ForwardResolution(VIEW);
  }

  /* ================= VIEW ================= */

  public Resolution viewOrder() {
    HttpSession session = context.getRequest().getSession();

    AccountActionBean account = (AccountActionBean) session.getAttribute("/actions/Account.action");

    order = orderService.getOrder(order.getOrderId());

    if (account == null || order == null || !account.getAccount().getUsername().equals(order.getUsername())) {
      setMessage("Unauthorized order access.");
      return new ForwardResolution(ERROR);
    }

    return new ForwardResolution(VIEW);
  }

  /* ================= GETTERS/SETTERS ================= */

  public Order getOrder() {
    return order;
  }

  public void setOrder(Order order) {
    this.order = order;
  }

  public boolean isConfirmed() {
    return confirmed;
  }

  public void setConfirmed(boolean confirmed) {
    this.confirmed = confirmed;
  }

  public List<String> getCreditCardTypes() {
    return CARD_TYPES;
  }
}
