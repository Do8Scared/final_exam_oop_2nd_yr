import { X, Plus, Minus, ShoppingBag, ChevronRight } from "lucide-react";
import { motion, AnimatePresence } from "motion/react";

export type CartItem = {
  id: number;
  name: string;
  price: number;
  qty: number;
  image: string;
};

type CartProps = {
  items: CartItem[];
  onClose: () => void;
  onAdd: (id: number) => void;
  onRemove: (id: number) => void;
  onDelete: (id: number) => void;
  onCheckout: () => void;
};

export function Cart({ items, onClose, onAdd, onRemove, onDelete, onCheckout }: CartProps) {
  const total = items.reduce((sum, i) => sum + i.price * i.qty, 0);

  return (
    <motion.div
      initial={{ x: "100%" }}
      animate={{ x: 0 }}
      exit={{ x: "100%" }}
      transition={{ type: "spring", damping: 28, stiffness: 300 }}
      className="fixed inset-y-0 right-0 z-50 w-full max-w-sm flex flex-col"
      style={{ background: "#1c1b19", borderLeft: "1px solid rgba(200,147,42,0.2)" }}
    >
      {/* Header */}
      <div className="flex items-center justify-between px-6 py-5 border-b border-border">
        <div className="flex items-center gap-3">
          <ShoppingBag size={20} style={{ color: "#c8932a" }} />
          <span style={{ fontFamily: "'Oswald', sans-serif", fontSize: "1.2rem", color: "#f0ede8", letterSpacing: "0.05em" }}>
            YOUR ORDER
          </span>
        </div>
        <button onClick={onClose} className="text-muted-foreground hover:text-foreground transition-colors">
          <X size={20} />
        </button>
      </div>

      {/* Items */}
      <div className="flex-1 overflow-y-auto px-6 py-4 space-y-4">
        <AnimatePresence>
          {items.length === 0 && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              className="flex flex-col items-center justify-center h-full gap-4 py-16"
            >
              <ShoppingBag size={48} style={{ color: "rgba(200,147,42,0.3)" }} />
              <p style={{ color: "#8a8070", fontFamily: "'Inter', sans-serif" }}>Your cart is empty</p>
            </motion.div>
          )}
          {items.map((item) => (
            <motion.div
              key={item.id}
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, x: 40 }}
              className="flex items-center gap-4 p-3 rounded-lg"
              style={{ background: "#242320" }}
            >
              <img src={item.image} alt={item.name} className="w-16 h-16 rounded-md object-cover flex-shrink-0" />
              <div className="flex-1 min-w-0">
                <p style={{ fontFamily: "'Oswald', sans-serif", color: "#f0ede8", fontSize: "0.95rem", letterSpacing: "0.03em" }} className="truncate">
                  {item.name}
                </p>
                <p style={{ color: "#c8932a", fontFamily: "'Inter', sans-serif", fontSize: "0.85rem", marginTop: "2px" }}>
                  ₱{(item.price * item.qty).toFixed(2)}
                </p>
              </div>
              <div className="flex items-center gap-2">
                <button
                  onClick={() => onRemove(item.id)}
                  className="w-7 h-7 rounded flex items-center justify-center transition-colors"
                  style={{ background: "#2a2924", color: "#c8932a" }}
                >
                  <Minus size={13} />
                </button>
                <span style={{ color: "#f0ede8", fontFamily: "'Oswald', sans-serif", minWidth: "18px", textAlign: "center" }}>
                  {item.qty}
                </span>
                <button
                  onClick={() => onAdd(item.id)}
                  className="w-7 h-7 rounded flex items-center justify-center transition-colors"
                  style={{ background: "#c8932a", color: "#111110" }}
                >
                  <Plus size={13} />
                </button>
              </div>
              <button onClick={() => onDelete(item.id)} className="text-muted-foreground hover:text-foreground ml-1 transition-colors">
                <X size={15} />
              </button>
            </motion.div>
          ))}
        </AnimatePresence>
      </div>

      {/* Footer */}
      {items.length > 0 && (
        <div className="px-6 py-5 border-t border-border space-y-4">
          <div className="flex items-center justify-between">
            <span style={{ color: "#8a8070", fontFamily: "'Inter', sans-serif", fontSize: "0.9rem" }}>Subtotal</span>
            <span style={{ color: "#f0ede8", fontFamily: "'Oswald', sans-serif", fontSize: "1.1rem" }}>₱{total.toFixed(2)}</span>
          </div>
          <button
            onClick={onCheckout}
            className="w-full flex items-center justify-center gap-2 py-4 rounded-lg transition-all hover:brightness-110 active:scale-[0.98]"
            style={{ background: "#c8932a", color: "#111110", fontFamily: "'Oswald', sans-serif", fontSize: "1rem", letterSpacing: "0.08em" }}
          >
            CHECKOUT <ChevronRight size={18} />
          </button>
        </div>
      )}
    </motion.div>
  );
}
