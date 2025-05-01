package com.ecommerce.ecommerce_backend.Service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentService {

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @PostConstruct
    public void init() {
        if (stripeSecretKey == null || stripeSecretKey.trim().isEmpty()) {
            throw new IllegalStateException("Stripe API key is not configured in application.properties");
        }
        Stripe.apiKey = stripeSecretKey;
    }

    public PaymentIntent createPaymentIntent(double amount, String currency) throws StripeException {
        Map<String, Object> params = new HashMap<>();
        params.put("amount", (long) (amount * 100)); // Amount in cents
        params.put("currency", currency);
        params.put("payment_method_types", new String[]{"card"});
        return PaymentIntent.create(params);
    }

    public PaymentIntent confirmPaymentIntent(String paymentIntentId) throws StripeException {
        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
        if ("requires_confirmation".equals(paymentIntent.getStatus()) || "requires_action".equals(paymentIntent.getStatus())) {
            paymentIntent = paymentIntent.confirm(); // Confirm if needed
        } else if (!"succeeded".equals(paymentIntent.getStatus())) {
            throw new IllegalStateException("Payment intent status invalid: " + paymentIntent.getStatus());
        }
        return paymentIntent;
    }
}