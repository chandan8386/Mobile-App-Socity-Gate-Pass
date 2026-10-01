package com.example.service.payment

import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

/**
 * Payment method providers supported by the mock payment gateway.
 */
enum class PaymentMethodType(val displayName: String, val category: String) {
    UPI_GPAY("Google Pay (UPI)", "UPI"),
    UPI_PHONEPE("PhonePe (UPI)", "UPI"),
    UPI_PAYTM("Paytm (UPI)", "UPI"),
    CREDIT_CARD("Credit Card (Visa / MasterCard)", "CARD"),
    DEBIT_CARD("Debit Card (RuPay)", "CARD"),
    NET_BANKING("NetBanking (HDFC / SBI / ICICI)", "NETBANKING"),
    RAZORPAY_WALLET("Razorpay Smart Wallet", "WALLET")
}

/**
 * Status of payment transaction.
 */
enum class PaymentStatus {
    INITIATED,
    PROCESSING,
    SUCCESS,
    FAILED
}

/**
 * Data payload for requesting a maintenance bill payment transaction.
 */
data class PaymentTransactionRequest(
    val billId: Long,
    val flatNumber: String,
    val residentName: String,
    val monthYear: String,
    val amount: Double,
    val paymentMethod: PaymentMethodType,
    val payerVpaOrCardNumber: String = "",
    val simulateBankDecline: Boolean = false
)

/**
 * Detailed result capturing the transaction ID, status, receipt, and payment audit trail.
 */
data class PaymentTransactionResult(
    val isSuccess: Boolean,
    val status: PaymentStatus,
    val transactionId: String,
    val receiptNumber: String,
    val bankRefNumber: String,
    val gatewayProvider: String,
    val billId: Long,
    val flatNumber: String,
    val amountPaid: Double,
    val timestamp: Long,
    val errorMessage: String? = null
)

/**
 * Service that simulates a real-world payment gateway (e.g. Razorpay / PayU / BillDesk).
 * Captures unique transaction reference IDs, bank authorization codes, and official receipt numbers
 * for persistent storage in the Room database maintenance billing ledger.
 */
object MockPaymentGatewayService {

    /**
     * Executes the payment simulation with realistic gateway authorization stages.
     */
    suspend fun executePayment(
        request: PaymentTransactionRequest,
        onStageUpdate: ((String) -> Unit)? = null
    ): PaymentTransactionResult {
        onStageUpdate?.invoke("Initiating 256-bit SSL session with payment gateway...")
        delay(400)

        onStageUpdate?.invoke("Connecting to ${request.paymentMethod.displayName} network...")
        delay(500)

        onStageUpdate?.invoke("Awaiting bank authorization & 2-factor authentication...")
        delay(600)

        val now = System.currentTimeMillis()
        val datePrefix = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(now))
        val randomSuffix = Random.nextInt(100000, 999999).toString()

        // Handle intentional simulation of bank failure
        if (request.simulateBankDecline) {
            val failedTxnId = "TXN-FAIL-$datePrefix-$randomSuffix"
            return PaymentTransactionResult(
                isSuccess = false,
                status = PaymentStatus.FAILED,
                transactionId = failedTxnId,
                receiptNumber = "NONE",
                bankRefNumber = "ERR-BANK-DECLINE-501",
                gatewayProvider = request.paymentMethod.displayName,
                billId = request.billId,
                flatNumber = request.flatNumber,
                amountPaid = request.amount,
                timestamp = now,
                errorMessage = "Card or Bank declined transaction. Insufficient funds or invalid OTP."
            )
        }

        onStageUpdate?.invoke("Payment authorized! Generating transaction ID and official receipt...")
        delay(300)

        // Generate unique cryptographically formatted IDs
        val methodCode = when (request.paymentMethod.category) {
            "UPI" -> "UPI"
            "CARD" -> "CARD"
            "NETBANKING" -> "NB"
            else -> "RZP"
        }
        val transactionId = "TXN-${methodCode}-$datePrefix-$randomSuffix"
        val bankRef = "BNK-" + UUID.randomUUID().toString().take(8).uppercase()
        val flatCode = request.flatNumber.replace("-", "")
        val receiptNumber = "REC-SG-${datePrefix}-${flatCode}-${randomSuffix.take(4)}"

        return PaymentTransactionResult(
            isSuccess = true,
            status = PaymentStatus.SUCCESS,
            transactionId = transactionId,
            receiptNumber = receiptNumber,
            bankRefNumber = bankRef,
            gatewayProvider = request.paymentMethod.displayName,
            billId = request.billId,
            flatNumber = request.flatNumber,
            amountPaid = request.amount,
            timestamp = now
        )
    }
}
