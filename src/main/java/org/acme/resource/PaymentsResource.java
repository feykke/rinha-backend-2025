package org.acme.resource;

import io.netty.handler.codec.http.HttpResponseStatus;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import org.acme.dto.PaymentRequestDTO;
import org.acme.entity.PaymentsSummaryResponse;
import org.acme.service.PaymentsService;


@Path("/")
public class PaymentsResource {

    private final PaymentsService paymentsService;

    @Inject
    public PaymentsResource(PaymentsService paymentsService) {
        this.paymentsService = paymentsService;
    }

    @Path("payments-summary")
    @GET
    public Response getPaymentsSummary(
            @QueryParam("from") final String from,
            @QueryParam("to") final String to
    ) {

        PaymentsSummaryResponse summary = paymentsService.getPaymentsSummary(from, to);
        return Response.status(HttpResponseStatus.OK.code()).entity(summary).build();
    }

    @Path("payments")
    @POST
    public Response postPayment(PaymentRequestDTO payment) {
        paymentsService.enqueuePayment(payment);
        return Response.status(HttpResponseStatus.ACCEPTED.code()).build();
    }
}
