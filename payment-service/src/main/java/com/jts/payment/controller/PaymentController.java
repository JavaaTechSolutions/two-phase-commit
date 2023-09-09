package com.jts.payment.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {

	@Autowired
	private PaymentRepository paymentRepository;

	@PostMapping("/prepare_payment")
	public ResponseEntity<String> prepareOrder(@RequestBody TransactionData transactionData) {
		try {
			Payment payment = new Payment();
			payment.setOrderNumber(transactionData.getOrderNumber());
			payment.setItem(transactionData.getItem());
			payment.setPreparationStatus(PaymentStatus.PENDING.name());
			payment.setPrice(transactionData.getPrice());
			payment.setPaymentMode(transactionData.getPaymentMode());
			paymentRepository.save(payment);

			if (shouldFailedDuringPrepare()) {
				throw new RuntimeException("Prepare phase failed for payment " + transactionData.getOrderNumber());
			}

			return ResponseEntity.ok("Payment prepared successfully");
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during payment preparation");
		}
	}

	private boolean shouldFailedDuringPrepare() {
		return false;
	}

	@PostMapping("/commit_payment")
	public ResponseEntity<String> commitOrder(@RequestBody TransactionData transactionData) {
		Payment order = paymentRepository.findByItem(transactionData.getItem());

		if (order != null && order.getPreparationStatus().equalsIgnoreCase(PaymentStatus.PENDING.name())) {
			order.setPreparationStatus(PaymentStatus.APPROVED.name());
			paymentRepository.save(order);

			return ResponseEntity.ok("Payment committed successfully");
		}

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Payment cannot be committed");
	}

	@PostMapping("/rollback_payment")
	public ResponseEntity<String> rollbackOrder(@RequestBody TransactionData transactionData) {
		Payment order = paymentRepository.findByItem(transactionData.getItem());

		if (order != null) {
			order.setPreparationStatus(PaymentStatus.ROLLBACK.name());
			paymentRepository.save(order);

			return ResponseEntity.ok("Payment rolled back successfully");
		}

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during Payment rollback");
	}

}
