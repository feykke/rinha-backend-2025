package org.acme.controller;

import io.netty.handler.codec.http.HttpResponseStatus;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.acme.dto.PaymentsSummaryResponseDTO;
import org.acme.entity.Payment;


@Path("/")
public class PaymentsController {

    @Path("payments-summary")
    @GET
    public Response getPaymentsSummary() {
        PaymentsSummaryResponseDTO summary = new PaymentsSummaryResponseDTO(12, 14, 150.0, 31.00);
        return Response.status(HttpResponseStatus.OK.code()).entity(summary).build();
    }

    @Path("payments")
    @POST
    public Response postPayment(Payment payment) {
        return Response.status(HttpResponseStatus.CREATED.code()).entity(payment).build();
    }
}
