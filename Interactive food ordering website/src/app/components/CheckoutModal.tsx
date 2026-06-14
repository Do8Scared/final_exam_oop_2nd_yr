import { useState } from "react";
import { X, ChevronRight, ChevronLeft, CheckCircle, MapPin, CreditCard, Bike, Receipt } from "lucide-react";
import { motion, AnimatePresence } from "motion/react";
import type { CartItem } from "./Cart";
import type { OrderReceipt } from "./ReceiptModal";

type CheckoutProps = {
  items: CartItem[];
  onClose: () => void;
  onSuccess: (receipt: OrderReceipt) => void;
  user: { name: string; email: string } | null;
};

type FormData = {
  name: string;
  email: string;
  phone: string;
  address: string;
  notes: string;
  paymentMethod: "cash" | "gcash" | "card";
};

const DELIVERY_FEE = 50;

export function CheckoutModal({ items, onClose, onSuccess, user }: CheckoutProps) {
  const [step, setStep] = useState<1 | 2 | 3>(1);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [form, setForm] = useState<FormData>({
    name: user?.name ?? "",
    email: user?.email ?? "",
    phone: "",
    address: "",
    notes: "",
    paymentMethod: "cash",
  });
  const [errors, setErrors] = useState<Partial<FormData>>({});

  const subtotal = items.reduce((s, i) => s + i.price * i.qty, 0);
  const total = subtotal + DELIVERY_FEE;

  const validate = () => {
    const e: Partial<FormData> = {};
    if (!form.name.trim()) e.name = "Required";
    if (!form.email.trim()) e.email = "Required";
    if (!form.phone.trim()) e.phone = "Required";
    if (!form.address.trim()) e.address = "Required";
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleNext = async () => {
    if (step === 1 && validate()) setStep(2);
    else if (step === 2) {
      setIsSubmitting(true);
      
      const payload = {
        orderType: "Delivery",
        paymentMethod: form.paymentMethod === "cash" ? "Cash" : form.paymentMethod === "gcash" ? "GCash" : "Card",
        additionalFee: DELIVERY_FEE,
        email: user?.email || "",
        customerName: form.name,
        contactNumber: form.phone,
        deliverTo: form.address,
        notes: form.notes,
        items: items.map(i => ({
          menuItemId: i.id,
          quantity: i.qty
        }))
      };

      try {
        const response = await fetch("http://localhost:8081/api/checkout", {
          method: "POST",
          headers: {
            "Content-Type": "application/json"
          },
          body: JSON.stringify(payload)
        });

        if (response.ok) {
          const responseData = await response.json();
          const now = new Date();
          const receipt: OrderReceipt = {
            orderNumber: responseData.transactionId || Math.random().toString(36).substring(2, 10).toUpperCase(),
            date: now.toLocaleDateString("en-PH", { year: "numeric", month: "long", day: "numeric" }),
            time: now.toLocaleTimeString("en-PH", { hour: "2-digit", minute: "2-digit" }),
            customer: { name: form.name, email: form.email, phone: form.phone, address: form.address },
            items,
            paymentMethod: form.paymentMethod,
            subtotal,
            deliveryFee: DELIVERY_FEE,
            total,
            notes: form.notes || undefined,
          };
          onSuccess(receipt);
          setStep(3);
        } else {
          try {
            const errorMsg = await response.json();
            alert("Checkout failed: " + errorMsg.error);
          } catch {
            alert("Checkout failed.");
          }
        }
      } catch (err) {
        console.error("Checkout error:", err);
        alert("Checkout failed. Please check your connection to the server.");
      } finally {
        setIsSubmitting(false);
      }
    }
  };

  const field = (label: string, key: keyof FormData, type = "text", placeholder = "") => (
    <div className="flex flex-col gap-1">
      <label style={{ color: "#8a8070", fontSize: "0.75rem", letterSpacing: "0.08em", fontFamily: "'Oswald', sans-serif" }}>
        {label}
      </label>
      <input
        type={type}
        value={form[key] as string}
        onChange={(e) => setForm((f) => ({ ...f, [key]: e.target.value }))}
        placeholder={placeholder}
        className="w-full px-4 py-3 rounded-lg outline-none transition-all"
        style={{
          background: "#242320",
          border: `1px solid ${errors[key] ? "#c0392b" : "rgba(200,147,42,0.2)"}`,
          color: "#f0ede8",
          fontFamily: "'Inter', sans-serif",
          fontSize: "0.9rem",
        }}
      />
      {errors[key] && <span style={{ color: "#c0392b", fontSize: "0.75rem" }}>{errors[key]}</span>}
    </div>
  );

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
        className="relative w-full max-w-2xl max-h-[90vh] overflow-y-auto rounded-2xl flex flex-col"
        style={{ background: "#1c1b19", border: "1px solid rgba(200,147,42,0.2)" }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-5 border-b" style={{ borderColor: "rgba(200,147,42,0.15)" }}>
          <div className="flex items-center gap-3">
            {step > 1 && step < 3 && (
              <button onClick={() => setStep((s) => (s - 1) as 1 | 2)} className="text-muted-foreground hover:text-foreground transition-colors mr-1">
                <ChevronLeft size={18} />
              </button>
            )}
            <span style={{ fontFamily: "'Oswald', sans-serif", color: "#f0ede8", fontSize: "1.2rem", letterSpacing: "0.05em" }}>
              {step === 1 ? "DELIVERY DETAILS" : step === 2 ? "ORDER SUMMARY" : "ORDER PLACED!"}
            </span>
          </div>
          <button onClick={onClose} className="text-muted-foreground hover:text-foreground transition-colors">
            <X size={20} />
          </button>
        </div>

        {/* Steps indicator */}
        {step < 3 && (
          <div className="flex items-center px-6 py-4 gap-2">
            {[1, 2].map((s) => (
              <div key={s} className="flex items-center gap-2 flex-1">
                <div
                  className="w-6 h-6 rounded-full flex items-center justify-center text-xs flex-shrink-0"
                  style={{
                    background: step >= s ? "#c8932a" : "#242320",
                    color: step >= s ? "#111110" : "#8a8070",
                    fontFamily: "'Oswald', sans-serif",
                    fontSize: "0.75rem",
                  }}
                >
                  {s}
                </div>
                <span style={{ color: step >= s ? "#c8932a" : "#8a8070", fontSize: "0.75rem", fontFamily: "'Inter', sans-serif" }}>
                  {s === 1 ? "Details" : "Review"}
                </span>
                {s < 2 && <div className="flex-1 h-px mx-2" style={{ background: step > s ? "#c8932a" : "rgba(200,147,42,0.2)" }} />}
              </div>
            ))}
          </div>
        )}

        {/* Content */}
        <div className="px-6 py-4 flex-1">
          <AnimatePresence mode="wait">
            {step === 1 && (
              <motion.div key="step1" initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -20 }} className="space-y-4">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  {field("FULL NAME", "name", "text", "Juan dela Cruz")}
                  {field("EMAIL", "email", "email", "juan@email.com")}
                </div>
                {field("PHONE NUMBER", "phone", "tel", "+63 912 345 6789")}
                {field("DELIVERY ADDRESS", "address", "text", "Street, Barangay, City")}
                <div className="flex flex-col gap-1">
                  <label style={{ color: "#8a8070", fontSize: "0.75rem", letterSpacing: "0.08em", fontFamily: "'Oswald', sans-serif" }}>ORDER NOTES (OPTIONAL)</label>
                  <textarea
                    value={form.notes}
                    onChange={(e) => setForm((f) => ({ ...f, notes: e.target.value }))}
                    placeholder="Any special requests?"
                    rows={3}
                    className="w-full px-4 py-3 rounded-lg outline-none resize-none transition-all"
                    style={{ background: "#242320", border: "1px solid rgba(200,147,42,0.2)", color: "#f0ede8", fontFamily: "'Inter', sans-serif", fontSize: "0.9rem" }}
                  />
                </div>
                {/* Payment method */}
                <div className="flex flex-col gap-2">
                  <label style={{ color: "#8a8070", fontSize: "0.75rem", letterSpacing: "0.08em", fontFamily: "'Oswald', sans-serif" }}>PAYMENT METHOD</label>
                  <div className="grid grid-cols-3 gap-3">
                    {[
                      { key: "cash", label: "Cash on Delivery", icon: <Bike size={18} /> },
                      { key: "gcash", label: "GCash", icon: <CreditCard size={18} /> },
                      { key: "card", label: "Card", icon: <CreditCard size={18} /> },
                    ].map(({ key, label, icon }) => (
                      <button
                        key={key}
                        onClick={() => setForm((f) => ({ ...f, paymentMethod: key as FormData["paymentMethod"] }))}
                        className="flex flex-col items-center gap-2 p-3 rounded-lg transition-all text-center"
                        style={{
                          background: form.paymentMethod === key ? "rgba(200,147,42,0.15)" : "#242320",
                          border: `1px solid ${form.paymentMethod === key ? "#c8932a" : "rgba(200,147,42,0.15)"}`,
                          color: form.paymentMethod === key ? "#c8932a" : "#8a8070",
                        }}
                      >
                        {icon}
                        <span style={{ fontFamily: "'Inter', sans-serif", fontSize: "0.72rem" }}>{label}</span>
                      </button>
                    ))}
                  </div>
                </div>
              </motion.div>
            )}

            {step === 2 && (
              <motion.div key="step2" initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -20 }} className="space-y-5">
                {/* Delivery info */}
                <div className="p-4 rounded-xl space-y-2" style={{ background: "#242320" }}>
                  <div className="flex items-center gap-2 mb-3">
                    <MapPin size={15} style={{ color: "#c8932a" }} />
                    <span style={{ fontFamily: "'Oswald', sans-serif", color: "#f0ede8", fontSize: "0.9rem", letterSpacing: "0.04em" }}>DELIVERY TO</span>
                  </div>
                  <p style={{ color: "#f0ede8", fontSize: "0.9rem" }}>{form.name}</p>
                  <p style={{ color: "#8a8070", fontSize: "0.82rem" }}>{form.address}</p>
                  <p style={{ color: "#8a8070", fontSize: "0.82rem" }}>{form.phone}</p>
                </div>
                {/* Items */}
                <div className="space-y-3">
                  {items.map((item) => (
                    <div key={item.id} className="flex items-center gap-3">
                      <img src={item.image} alt={item.name} className="w-12 h-12 rounded-lg object-cover" />
                      <div className="flex-1">
                        <p style={{ color: "#f0ede8", fontFamily: "'Oswald', sans-serif", fontSize: "0.9rem", letterSpacing: "0.03em" }}>{item.name}</p>
                        <p style={{ color: "#8a8070", fontSize: "0.8rem" }}>x{item.qty}</p>
                      </div>
                      <p style={{ color: "#c8932a", fontFamily: "'Oswald', sans-serif" }}>₱{(item.price * item.qty).toFixed(2)}</p>
                    </div>
                  ))}
                </div>
                {/* Totals */}
                <div className="pt-3 border-t space-y-2" style={{ borderColor: "rgba(200,147,42,0.15)" }}>
                  {[{ label: "Subtotal", val: subtotal }, { label: "Delivery Fee", val: DELIVERY_FEE }].map(({ label, val }) => (
                    <div key={label} className="flex justify-between">
                      <span style={{ color: "#8a8070", fontSize: "0.85rem" }}>{label}</span>
                      <span style={{ color: "#f0ede8", fontFamily: "'Oswald', sans-serif" }}>₱{val.toFixed(2)}</span>
                    </div>
                  ))}
                  <div className="flex justify-between pt-2 border-t" style={{ borderColor: "rgba(200,147,42,0.15)" }}>
                    <span style={{ color: "#f0ede8", fontFamily: "'Oswald', sans-serif", fontSize: "1.05rem" }}>TOTAL</span>
                    <span style={{ color: "#c8932a", fontFamily: "'Oswald', sans-serif", fontSize: "1.1rem" }}>₱{total.toFixed(2)}</span>
                  </div>
                </div>
              </motion.div>
            )}

            {step === 3 && (
              <motion.div key="step3" initial={{ opacity: 0, scale: 0.9 }} animate={{ opacity: 1, scale: 1 }} className="flex flex-col items-center text-center py-10 gap-5">
                <motion.div initial={{ scale: 0 }} animate={{ scale: 1 }} transition={{ delay: 0.2, type: "spring", stiffness: 200 }}>
                  <CheckCircle size={72} style={{ color: "#c8932a" }} />
                </motion.div>
                <div>
                  <h3 style={{ fontFamily: "'Oswald', sans-serif", color: "#f0ede8", fontSize: "1.8rem", letterSpacing: "0.06em" }}>ORDER CONFIRMED!</h3>
                  <p style={{ color: "#8a8070", marginTop: "8px", fontSize: "0.9rem" }}>
                    Thank you, {form.name}! Your order is being prepared.
                  </p>
                  <p style={{ color: "#8a8070", fontSize: "0.85rem", marginTop: "4px" }}>
                    Estimated delivery: 30–45 minutes
                  </p>
                </div>
                <div className="p-4 rounded-xl w-full" style={{ background: "#242320" }}>
                  <p style={{ color: "#8a8070", fontSize: "0.8rem" }}>Delivering to</p>
                  <p style={{ color: "#f0ede8", fontSize: "0.9rem", marginTop: "4px" }}>{form.address}</p>
                  <p style={{ color: "#c8932a", fontSize: "0.85rem", marginTop: "4px" }}>
                    Payment: {form.paymentMethod === "cash" ? "Cash on Delivery" : form.paymentMethod === "gcash" ? "GCash" : "Card"}
                  </p>
                </div>
                <div className="flex flex-col gap-3 w-full">
                  <button
                    onClick={onClose}
                    className="w-full py-4 rounded-lg flex items-center justify-center gap-2 transition-all hover:brightness-110"
                    style={{ background: "#c8932a", color: "#111110", fontFamily: "'Oswald', sans-serif", letterSpacing: "0.08em" }}
                  >
                    <Receipt size={16} /> VIEW RECEIPT
                  </button>
                  <button
                    onClick={onClose}
                    className="w-full py-3 rounded-lg transition-all"
                    style={{ background: "transparent", color: "#8a8070", fontFamily: "'Oswald', sans-serif", letterSpacing: "0.06em", fontSize: "0.85rem", border: "1px solid rgba(200,147,42,0.2)" }}
                  >
                    BACK TO MENU
                  </button>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        {/* Footer CTA */}
        {step < 3 && (
          <div className="px-6 py-5 border-t" style={{ borderColor: "rgba(200,147,42,0.15)" }}>
            <button
              onClick={handleNext}
              disabled={isSubmitting}
              className="w-full flex items-center justify-center gap-2 py-4 rounded-lg transition-all hover:brightness-110 active:scale-[0.98]"
              style={{ background: "#c8932a", color: "#111110", fontFamily: "'Oswald', sans-serif", letterSpacing: "0.08em", opacity: isSubmitting ? 0.7 : 1 }}
            >
              {isSubmitting ? "PROCESSING..." : step === 1 ? "REVIEW ORDER" : "PLACE ORDER"}
              {!isSubmitting && <ChevronRight size={18} />}
            </button>
          </div>
        )}
      </motion.div>
    </div>
  );
}
