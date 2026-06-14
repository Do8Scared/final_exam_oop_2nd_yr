import { X, Printer, CheckCircle } from "lucide-react";
import { motion } from "motion/react";
import type { CartItem } from "./Cart";

export type OrderReceipt = {
  orderNumber: string;
  date: string;
  time: string;
  customer: { name: string; email: string; phone: string; address: string };
  items: CartItem[];
  paymentMethod: string;
  subtotal: number;
  deliveryFee: number;
  total: number;
  notes?: string;
};

type ReceiptModalProps = {
  receipt: OrderReceipt;
  onClose: () => void;
};

const paymentLabel: Record<string, string> = {
  cash: "Cash on Delivery",
  gcash: "GCash",
  card: "Credit / Debit Card",
};

export function ReceiptModal({ receipt, onClose }: ReceiptModalProps) {
  const handlePrint = () => window.print();

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        className="absolute inset-0"
        style={{ background: "rgba(17,17,16,0.85)", backdropFilter: "blur(8px)" }}
        onClick={onClose}
      />

      <motion.div
        initial={{ opacity: 0, scale: 0.95, y: 20 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.95, y: 20 }}
        className="relative w-full max-w-sm max-h-[90vh] overflow-y-auto rounded-2xl flex flex-col print:shadow-none print:rounded-none"
        style={{ background: "#f5f0e8" }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Actions (hidden on print) */}
        <div className="flex items-center justify-between px-5 py-4 print:hidden" style={{ background: "#1c1b19" }}>
          <div className="flex items-center gap-2">
            <CheckCircle size={16} style={{ color: "#c8932a" }} />
            <span style={{ fontFamily: "'Oswald', sans-serif", color: "#f0ede8", fontSize: "0.95rem", letterSpacing: "0.05em" }}>ORDER RECEIPT</span>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={handlePrint}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm transition-all hover:brightness-110"
              style={{ background: "#c8932a", color: "#111110", fontFamily: "'Oswald', sans-serif", letterSpacing: "0.05em", fontSize: "0.8rem" }}
            >
              <Printer size={13} /> PRINT
            </button>
            <button onClick={onClose} className="p-1.5 rounded-lg transition-colors" style={{ color: "#8a8070" }}>
              <X size={18} />
            </button>
          </div>
        </div>

        {/* Receipt body */}
        <div className="px-6 pt-6 pb-8 space-y-5" style={{ fontFamily: "'Inter', sans-serif", color: "#1a1814" }}>
          {/* Header */}
          <div className="text-center space-y-1 pb-4" style={{ borderBottom: "2px dashed #c8b89a" }}>
            <p style={{ fontFamily: "'Oswald', sans-serif", color: "#c8932a", fontSize: "0.7rem", letterSpacing: "0.22em" }}>OFFICIAL RECEIPT</p>
            <h2 style={{ fontFamily: "'Oswald', sans-serif", fontSize: "1.6rem", letterSpacing: "0.06em", color: "#1a1814", lineHeight: 1.1 }}>
              GARAHE NI<br />MATEICLA
            </h2>
            <p style={{ fontSize: "0.72rem", color: "#7a7060" }}>Mateicla, Philippines · +63 912 345 6789</p>
            <p style={{ fontSize: "0.72rem", color: "#7a7060" }}>Open 10AM – 10PM Daily</p>
          </div>

          {/* Order meta */}
          <div className="space-y-1.5">
            {[
              { label: "Order No.", value: `#${receipt.orderNumber}` },
              { label: "Date", value: receipt.date },
              { label: "Time", value: receipt.time },
              { label: "Customer", value: receipt.customer.name },
              { label: "Contact", value: receipt.customer.phone },
            ].map(({ label, value }) => (
              <div key={label} className="flex justify-between items-baseline gap-4">
                <span style={{ fontSize: "0.75rem", color: "#7a7060", minWidth: "70px" }}>{label}</span>
                <span style={{ fontSize: "0.8rem", fontWeight: 500, textAlign: "right" }}>{value}</span>
              </div>
            ))}
            <div className="flex justify-between items-start gap-4">
              <span style={{ fontSize: "0.75rem", color: "#7a7060", minWidth: "70px", flexShrink: 0 }}>Deliver To</span>
              <span style={{ fontSize: "0.8rem", fontWeight: 500, textAlign: "right" }}>{receipt.customer.address}</span>
            </div>
            {receipt.notes && (
              <div className="flex justify-between items-start gap-4">
                <span style={{ fontSize: "0.75rem", color: "#7a7060", minWidth: "70px", flexShrink: 0 }}>Notes</span>
                <span style={{ fontSize: "0.8rem", textAlign: "right", fontStyle: "italic", color: "#5a5040" }}>{receipt.notes}</span>
              </div>
            )}
          </div>

          {/* Items */}
          <div style={{ borderTop: "2px dashed #c8b89a", borderBottom: "2px dashed #c8b89a", paddingTop: "16px", paddingBottom: "16px" }} className="space-y-2.5">
            <div className="flex justify-between" style={{ fontSize: "0.68rem", color: "#7a7060", letterSpacing: "0.1em", textTransform: "uppercase", marginBottom: "8px" }}>
              <span>Item</span>
              <div className="flex gap-6">
                <span>Qty</span>
                <span style={{ minWidth: "60px", textAlign: "right" }}>Amount</span>
              </div>
            </div>
            {receipt.items.map((item) => (
              <div key={item.id} className="flex justify-between items-baseline gap-2">
                <span style={{ fontSize: "0.82rem", flex: 1 }}>{item.name}</span>
                <div className="flex gap-6 items-baseline flex-shrink-0">
                  <span style={{ fontSize: "0.78rem", color: "#7a7060", minWidth: "20px", textAlign: "center" }}>×{item.qty}</span>
                  <span style={{ fontSize: "0.82rem", minWidth: "60px", textAlign: "right" }}>₱{(item.price * item.qty).toFixed(2)}</span>
                </div>
              </div>
            ))}
          </div>

          {/* Totals */}
          <div className="space-y-1.5">
            <div className="flex justify-between">
              <span style={{ fontSize: "0.78rem", color: "#7a7060" }}>Subtotal</span>
              <span style={{ fontSize: "0.78rem" }}>₱{receipt.subtotal.toFixed(2)}</span>
            </div>
            <div className="flex justify-between">
              <span style={{ fontSize: "0.78rem", color: "#7a7060" }}>Delivery Fee</span>
              <span style={{ fontSize: "0.78rem" }}>₱{receipt.deliveryFee.toFixed(2)}</span>
            </div>
            <div className="flex justify-between pt-2" style={{ borderTop: "1px solid #c8b89a", marginTop: "8px" }}>
              <span style={{ fontFamily: "'Oswald', sans-serif", fontSize: "1rem", letterSpacing: "0.04em" }}>TOTAL</span>
              <span style={{ fontFamily: "'Oswald', sans-serif", fontSize: "1rem", color: "#c8932a" }}>₱{receipt.total.toFixed(2)}</span>
            </div>
            <div className="flex justify-between">
              <span style={{ fontSize: "0.75rem", color: "#7a7060" }}>Payment</span>
              <span style={{ fontSize: "0.78rem" }}>{paymentLabel[receipt.paymentMethod] ?? receipt.paymentMethod}</span>
            </div>
          </div>

          {/* Footer */}
          <div className="text-center space-y-2 pt-2" style={{ borderTop: "2px dashed #c8b89a" }}>
            <p style={{ fontFamily: "'Oswald', sans-serif", fontSize: "0.85rem", letterSpacing: "0.08em", color: "#c8932a" }}>SALAMAT! THANK YOU!</p>
            <p style={{ fontSize: "0.7rem", color: "#7a7060", lineHeight: 1.6 }}>
              Please keep this receipt as proof of your order.<br />
              For concerns call +63 912 345 6789.
            </p>
            {/* Barcode-style decoration */}
            <div className="flex justify-center gap-px mt-3">
              {Array.from({ length: 40 }).map((_, i) => (
                <div
                  key={i}
                  style={{
                    width: i % 3 === 0 ? "3px" : "1.5px",
                    height: i % 5 === 0 ? "28px" : "20px",
                    background: "#1a1814",
                    opacity: 0.5 + (i % 4) * 0.1,
                  }}
                />
              ))}
            </div>
            <p style={{ fontSize: "0.65rem", color: "#7a7060", letterSpacing: "0.15em" }}>{receipt.orderNumber}</p>
          </div>
        </div>

        {/* Close button */}
        <div className="px-5 pb-5 print:hidden">
          <button
            onClick={onClose}
            className="w-full py-3 rounded-xl transition-all hover:brightness-110"
            style={{ background: "#1c1b19", color: "#c8932a", fontFamily: "'Oswald', sans-serif", letterSpacing: "0.08em" }}
          >
            CLOSE RECEIPT
          </button>
        </div>
      </motion.div>
    </div>
  );
}
