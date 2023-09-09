package com.jts.order.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

	@Autowired
	private OrderRepository orderRepository;

	@PostMapping("/prepare_order")
	public ResponseEntity<String> prepareOrder(@RequestBody TransactionData transactionData) {
		try {
			Order order = new Order();
			order.setOrderNumber(transactionData.getOrderNumber());
			order.setItem(transactionData.getItem());
			order.setPreparationStatus(OrderPreparationStatus.PREPARING.name());
			orderRepository.save(order);

			if (shouldFailedDuringPrepare()) {
				throw new RuntimeException("Prepare phase failed for order " + transactionData.getOrderNumber());
			}

			return ResponseEntity.ok("Order prepared successfully");
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during oredr preparation");
		}
	}

	private boolean shouldFailedDuringPrepare() {
		return false;
	}

	@PostMapping("/commit_order")
	public ResponseEntity<String> commitOrder(@RequestBody TransactionData transactionData) {
		Order order = orderRepository.findByItem(transactionData.getItem());

		if (order != null && order.getPreparationStatus().equalsIgnoreCase(OrderPreparationStatus.PREPARING.name())) {
			order.setPreparationStatus(OrderPreparationStatus.COMMITTED.name());
			orderRepository.save(order);

			return ResponseEntity.ok("Order committed successfully");
		}

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Order cannot be committed");
	}

	@PostMapping("/rollback_order")
	public ResponseEntity<String> rollbackOrder(@RequestBody TransactionData transactionData) {
		Order order = orderRepository.findByItem(transactionData.getItem());

		if (order != null) {
			order.setPreparationStatus(OrderPreparationStatus.ROLLBACK.name());
			orderRepository.save(order);

			return ResponseEntity.ok("Order rolled back successfully");
		}

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during order rollback");
	}

}
