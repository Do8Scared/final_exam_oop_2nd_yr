import { useState, useEffect } from "react";
import { X, Receipt, Clock, Package, CreditCard, User as UserIcon, Eye } from "lucide-react";
import { motion, AnimatePresence } from "motion/react";

type HistoryModalProps = {
  user: { name: string; email: string };
  onClose: () => void;
  onViewReceipt: (txn: Transaction) => void;
};

export type Transaction = {
  transactionId: string;
  date: string;
  orderType: string;
  paymentMethod: string;
  totalAmount: number;
  customerName: string;
  deliverTo: string;
};

export function TransactionHistoryModal({ user, onClose, onViewReceipt }: HistoryModalProps) {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetch(`https://final-exam-oop-2nd-yr.onrender.com/api/checkout/history?email=${encodeURIComponent(user.email)}`)
      .then((res) => {
        if (!res.ok) throw new Error("Failed to fetch history");
        return res.json();
      })
      .then((data) => {
        setTransactions(data);
        setLoading(false);
      })
      .catch((err) => {
        setError(err.message);
        setLoading(false);
      });
  }, [user.email]);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        className="absolute inset-0"
        style={{ background: "rgba(17,17,16,0.8)", backdropFilter: "blur(6px)" }}
        onClick={onClose}
      />
      <motion.div
        initial={{ opacity: 0, scale: 0.96, y: 20 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.96, y: 20 }}
        className="relative w-full max-w-2xl max-h-[90vh] overflow-y-auto rounded-2xl flex flex-col shadow-2xl"
        style={{ background: "#1c1b19", border: "1px solid rgba(200,147,42,0.2)" }}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="sticky top-0 z-10 flex items-center justify-between p-6 border-b" style={{ borderColor: "rgba(200,147,42,0.15)", background: "#1c1b19" }}>
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full flex items-center justify-center" style={{ background: "rgba(200,147,42,0.1)", color: "#c8932a" }}>
              <Receipt size={20} />
            </div>
            <div>
              <h2 style={{ fontFamily: "'Oswald', sans-serif", color: "#f0ede8", fontSize: "1.3rem", letterSpacing: "0.05em" }}>MY ORDERS</h2>
              <p style={{ color: "#8a8070", fontSize: "0.85rem", fontFamily: "'Inter', sans-serif" }}>Past transactions for {user.name}</p>
            </div>
          </div>
          <button onClick={onClose} className="p-2 rounded-full transition-colors hover:bg-white/5" style={{ color: "#8a8070" }}>
            <X size={20} />
          </button>
        </div>

        <div className="p-6 overflow-y-auto">
          {loading ? (
            <div className="flex justify-center items-center py-12">
              <span style={{ color: "#c8932a" }}>Loading history...</span>
            </div>
          ) : error ? (
            <div className="flex justify-center items-center py-12">
              <span style={{ color: "#c0392b" }}>{error}</span>
            </div>
          ) : transactions.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-16 text-center">
              <Package size={48} style={{ color: "#8a8070", opacity: 0.5, marginBottom: "1rem" }} />
              <h3 style={{ color: "#f0ede8", fontFamily: "'Oswald', sans-serif", fontSize: "1.2rem", letterSpacing: "0.05em" }}>NO ORDERS YET</h3>
              <p style={{ color: "#8a8070", fontSize: "0.9rem", marginTop: "0.5rem" }}>Looks like you haven't placed any orders yet.</p>
            </div>
          ) : (
            <div className="flex flex-col gap-4">
              {transactions.map((txn) => (
                <div key={txn.transactionId} className="flex flex-col rounded-xl p-5" style={{ background: "#242320", border: "1px solid rgba(200,147,42,0.1)" }}>
                  <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-4 pb-4 border-b" style={{ borderColor: "rgba(200,147,42,0.1)" }}>
                    <div>
                      <div className="flex items-center gap-2 mb-1">
                        <span style={{ fontFamily: "'Oswald', sans-serif", color: "#c8932a", letterSpacing: "0.05em", fontSize: "1.1rem" }}>{txn.transactionId}</span>
                        <span className="px-2 py-0.5 rounded-full text-[0.7rem] uppercase tracking-wider" style={{ background: "rgba(200,147,42,0.1)", color: "#c8932a" }}>
                          {txn.orderType}
                        </span>
                      </div>
                      <div className="flex items-center gap-2 text-sm" style={{ color: "#8a8070" }}>
                        <Clock size={14} />
                        <span>{txn.date || "Unknown date"}</span>
                      </div>
                    </div>
                    <div className="text-left md:text-right flex flex-col md:items-end">
                      <div style={{ fontFamily: "'Oswald', sans-serif", color: "#f0ede8", fontSize: "1.2rem", letterSpacing: "0.05em" }}>
                        PHP {txn.totalAmount.toFixed(2)}
                      </div>
                      <div className="flex items-center gap-1.5 md:justify-end text-sm mt-1" style={{ color: "#8a8070" }}>
                        <CreditCard size={14} />
                        <span className="capitalize">{txn.paymentMethod}</span>
                      </div>
                    </div>
                  </div>
                  
                  <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 text-sm">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 flex-1">
                      <div className="flex items-start gap-2">
                        <UserIcon size={16} style={{ color: "#8a8070", marginTop: "2px" }} />
                        <div>
                          <div style={{ color: "#f0ede8" }}>{txn.customerName || "N/A"}</div>
                          <div style={{ color: "#8a8070", fontSize: "0.8rem" }}>Customer</div>
                        </div>
                      </div>
                      {txn.orderType === "Delivery" && (
                        <div className="flex items-start gap-2">
                          <Package size={16} style={{ color: "#8a8070", marginTop: "2px" }} />
                          <div>
                            <div style={{ color: "#f0ede8" }}>{txn.deliverTo || "N/A"}</div>
                            <div style={{ color: "#8a8070", fontSize: "0.8rem" }}>Delivery Address</div>
                          </div>
                        </div>
                      )}
                    </div>
                    
                    <button
                      onClick={() => onViewReceipt(txn)}
                      className="flex items-center justify-center gap-2 px-4 py-2 rounded-lg transition-all hover:brightness-110 md:w-auto w-full mt-2 md:mt-0"
                      style={{ background: "rgba(200,147,42,0.15)", color: "#c8932a", border: "1px solid rgba(200,147,42,0.3)", fontFamily: "'Oswald', sans-serif", letterSpacing: "0.05em", fontSize: "0.8rem" }}
                    >
                      <Eye size={14} /> VIEW RECEIPT
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </motion.div>
    </div>
  );
}
