import { Plus } from "lucide-react";
import { motion } from "motion/react";

type MenuItemProps = {
  id: number;
  name: string;
  description: string;
  price: number;
  image: string;
  tag?: string;
  onAdd: (id: number) => void;
};

export function MenuItem({ id, name, description, price, image, tag, onAdd }: MenuItemProps) {
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
        <div className="flex items-center justify-between mt-1">
          <span style={{ color: "#c8932a", fontFamily: "'Oswald', sans-serif", fontSize: "1.05rem" }}>
            ₱{price.toFixed(2)}
          </span>
          <button
            onClick={() => onAdd(id)}
            className="w-9 h-9 rounded-full flex items-center justify-center transition-all hover:brightness-110 active:scale-95"
            style={{ background: "#c8932a", color: "#111110" }}
          >
            <Plus size={17} />
          </button>
        </div>
      </div>
    </motion.div>
  );
}
