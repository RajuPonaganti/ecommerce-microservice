/*
 * package com.ecommerce.order.service;
 * 
 * import java.time.Instant; import java.util.UUID;
 * 
 * import org.springframework.stereotype.Service; import
 * org.springframework.transaction.annotation.Transactional;
 * 
 * import com.ecommerce.order.clients.InventoryFeignClient; import
 * com.ecommerce.order.clients.NotificationFeignClient; import
 * com.ecommerce.order.clients.PaymentFeignClient; import
 * com.ecommerce.order.enums.OrderState; import
 * com.ecommerce.order.model.OrderSagaState; import
 * com.ecommerce.order.repository.OrderSagaStateRepository;
 * 
 * import lombok.RequiredArgsConstructor; import lombok.extern.slf4j.Slf4j;
 * 
 * @Service
 * 
 * @RequiredArgsConstructor
 * 
 * @Slf4j public class OrderSagaOrchestrator { private final
 * OrderSagaStateRepository sagaStateRepository; private final
 * InventoryFeignClient inventoryClient; private final NotificationFeignClient
 * notificationClient; private final PaymentFeignClient paymentClient;
 * 
 * @Transactional public void startSaga(UUID orderId) { OrderSagaState oss = new
 * OrderSagaState(); oss.setStatus(OrderState.CREATED); oss.setOrderId(orderId);
 * oss.setUpdatedAt(Instant.now()); sagaStateRepository.save(oss); advance(oss);
 * }
 * 
 * @Transactional private void advance(OrderSagaState oss) { switch
 * (oss.getStatus()) { case CREATED->{
 * oss.setStatus(OrderState.INVENTORY_RESERVING); sagaStateRepository.save(oss);
 * reserveInventory(oss); } case INVENTORY_RESERVED->{
 * oss.setStatus(OrderState.PAYMENT_PROCESSING); sagaStateRepository.save(oss);
 * processPayment(oss);
 * 
 * } case PAYMENT_CONFIRMED->{ oss.setStatus(OrderState.COMPLETED);
 * sagaStateRepository.save(oss); processPayment(oss);
 * notificationClient.sendSuccess(oss.getOrderId()); }
 * 
 * 
 * default->log.warn("No advance logic for State {}", oss.getStatus()); }
 * 
 * }
 * 
 * private void processPayment(OrderSagaState oss) { try {
 * paymentClient.charge(oss.getOrderId());
 * oss.setStatus(OrderState.PAYMENT_CONFIRMED); sagaStateRepository.save(oss);
 * advance(oss); } catch (Exception e) {
 * oss.setStatus(OrderState.PAYMENT_FAILED); oss.setLastError(e.getMessage());
 * sagaStateRepository.save(oss); //compensating transcation
 * compensateInventory(oss);
 * 
 * } }
 * 
 * private void compensateInventory(OrderSagaState oss) {
 * inventoryClient.releaseInventory(oss.getOrderId());
 * oss.setStatus(OrderState.CANCELLED); sagaStateRepository.save(oss);
 * notificationClient.sendFailure(oss.getOrderId()); }
 * 
 * private void reserveInventory(OrderSagaState oss) { try {
 * inventoryClient.reserveInventory(oss.getOrderId());
 * oss.setStatus(OrderState.INVENTORY_RESERVED); sagaStateRepository.save(oss);
 * advance(oss); } catch (Exception e) {
 * oss.setStatus(OrderState.INVENTORY_RESERVE_FAILED);
 * oss.setLastError(e.getMessage()); sagaStateRepository.save(oss);
 * 
 * }
 * 
 * 
 * } }
 */