import { Plus } from "lucide-react";
import { motion } from "motion/react";

type MenuItemProps = {
  id: number;
  name: string;
  description: string;
  price: number;
  image: string;
  tag?: string;
  specialDetails?: string;
  stockQuantity?: number;
  onAdd: (id: number) => void;
};

export function MenuItem({ id, name, description, price, image, tag, specialDetails, stockQuantity, onAdd }: MenuItemProps) {
  return (
    <motion.div
      whileHover={{ y: -4 }}
      transition={{ type: "spring", stiffness: 300, damping: 24 }}
      className="rounded-xl overflow-hidden flex flex-col"
      style={{ background: "#1c1b19", border: "1px solid rgba(200,147,42,0.1)" }}
    >
      <div className="relative overflow-hidden" style={{ height: "180px" }}>
        <img src={image} alt={name} className="w-full h-full object-cover" />
        {tag && (
          <span
            className="absolute top-3 left-3 px-2 py-1 rounded text-xs"
            style={{ background: "#c8932a", color: "#111110", fontFamily: "'Oswald', sans-serif", letterSpacing: "0.06em", fontSize: "0.7rem" }}
          >
            {tag}
          </span>
        )}
      </div>
      <div className="p-4 flex flex-col gap-2 flex-1">
        <h3 style={{ fontFamily: "'Oswald', sans-serif", color: "#f0ede8", fontSize: "1rem", letterSpacing: "0.04em" }}>
          {name}
        </h3>
        <p style={{ fontFamily: "'Inter', sans-serif", color: "#8a8070", fontSize: "0.8rem", lineHeight: "1.5", flex: 1 }}>
          {description}
        </p>
        {specialDetails && (
          <p style={{ fontFamily: "'Inter', sans-serif", color: "#c8932a", fontSize: "0.75rem", fontStyle: "italic", marginTop: "-0.25rem", marginBottom: "0.5rem" }}>
            {specialDetails}
          </p>
        )}
        <div className="flex items-center justify-between mt-1">
          <div className="flex flex-col">
            <span style={{ color: "#c8932a", fontFamily: "'Oswald', sans-serif", fontSize: "1.05rem" }}>
              ₱{price.toFixed(2)}
            </span>
            {stockQuantity !== undefined && (
              <span style={{ color: "#8a8070", fontSize: "0.75rem", fontFamily: "'Inter', sans-serif", marginTop: "-2px" }}>
                {stockQuantity > 0 ? `${stockQuantity} in stock` : "Out of stock"}
              </span>
            )}
          </div>
          <button
            onClick={() => onAdd(id)}
            disabled={stockQuantity === 0}
            className={`w-9 h-9 rounded-full flex items-center justify-center transition-all ${stockQuantity === 0 ? 'opacity-50 cursor-not-allowed' : 'hover:brightness-110 active:scale-95'}`}
            style={{ background: "#c8932a", color: "#111110" }}
          >
            <Plus size={17} />
          </button>
        </div>
      </div>
    </motion.div>
  );
}
