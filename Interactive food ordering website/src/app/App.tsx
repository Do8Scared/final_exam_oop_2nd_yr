import { useState, useEffect } from "react";
import { ShoppingBag, MapPin, Clock, Phone, Star, ChevronDown, User, LogOut } from "lucide-react";
import { AnimatePresence, motion } from "motion/react";
import { Cart, type CartItem } from "./components/Cart";
import { MenuItem } from "./components/MenuItem";
import { CheckoutModal } from "./components/CheckoutModal";
import { LoginModal } from "./components/LoginModal";
import { ReceiptModal, type OrderReceipt } from "./components/ReceiptModal";
import { TransactionHistoryModal } from "./components/TransactionHistoryModal";

// Original images
import jpancake from "../imports/Japanese_Pancake.jpg";
import kakitama from "../imports/Kakitamajiru.jpg";
import ajitama from "../imports/Ajitama_Egg.jpg";
import daifuku from "../imports/Daifuku.jpg";
import dango from "../imports/Dango.jpg";
import porkChashu from "../imports/Extra_Pork_Chashu.jpg";
import friedEgg from "../imports/Fried_Egg.jpg";
import gyudon from "../imports/Gyudon.jpg";
import hotSencha from "../imports/Hot_Sencha.jpg";
import cornSoup from "../imports/Japanese_Corn_Soup.jpg";
// New images batch 2
import ramune from "../imports/Ramune.jpg";
import shreddedNori from "../imports/Shredded_Nori.jpg";
import unagiDon from "../imports/Unagi_Don.jpg";
import ujiMatcha from "../imports/Uji_Matcha_Latte.jpg";
import steamedRice from "../imports/Steamed_Rice.jpg";
import tendon from "../imports/Tendon.jpg";
import yuzuTea from "../imports/Yuzu_Tea.jpg";
import yuzuSparkler from "../imports/uzu_Sparkler.jpg";
// New images batch 1
import porkCashu2 from "../imports/porkcashu.jpg";
import kaisendon from "../imports/Kaisendon.jpg";
import katsudon from "../imports/Katsudon.jpg";
import kabochaS from "../imports/Kabocha_Soup.jpg";
import mugicha from "../imports/Mugi-cha.jpg";
import kakigori from "../imports/Kakigori.jpg";
import oyakodon from "../imports/Oyakodon.jpg";
import mochi from "../imports/Mochi.jpg";
import misoSoup from "../imports/misosoup.jpg";
import edamame from "../imports/Pan-Fried_Edamame.jpg";
// Missing items batch
import kyuriTsukemono from "../imports/kyuri tsukemono.jpg";
import ramyon from "../imports/ramyon.jpg";
import spicyChiliRayu from "../imports/spicy chili rayu.jpg";
import yakitoriChicken from "../imports/yakitorichicken.jpg";

type MenuEntry = {
  id: number;
  name: string;
  description: string;
  price: number;
  image: string;
  category: string;
  tag?: string;
};

const STATIC_MENU: MenuEntry[] = [
  { id: 1, name: "Japanese Pancake", description: "Fluffy souffle-style pancake served with maple syrup and fresh cream.", price: 195, image: jpancake as string, category: "Mains", tag: "Best Seller" },
  { id: 2, name: "Kakitamajiru", description: "Traditional Japanese egg-drop soup with silky dashi broth and scallions.", price: 120, image: kakitama as string, category: "Soups" },
  { id: 3, name: "Ajitama Egg", description: "Marinated soft-boiled egg with soy, mirin and a perfectly jammy center.", price: 75, image: ajitama as string, category: "Add-ons", tag: "Popular" },
  { id: 4, name: "Daifuku", description: "Soft mochi filled with sweet red bean paste — a classic Japanese confection.", price: 90, image: daifuku as string, category: "Desserts" },
  { id: 5, name: "Dango", description: "Skewered mochi dumplings glazed with sweet soy sauce mitarashi.", price: 85, image: dango as string, category: "Desserts", tag: "Seasonal" },
  { id: 6, name: "Extra Pork Chashu", description: "Tender braised pork belly sliced and torched to order.", price: 110, image: porkChashu as string, category: "Add-ons" },
  { id: 7, name: "Fried Egg", description: "Farm-fresh fried egg with crispy edges, perfect as a topping.", price: 50, image: friedEgg as string, category: "Add-ons" },
  { id: 8, name: "Gyudon", description: "Slow-simmered beef and onion rice bowl with a sweet savory tare.", price: 225, image: gyudon as string, category: "Mains", tag: "Best Seller" },
  { id: 9, name: "Hot Sencha", description: "Premium Japanese green tea with a grassy, refreshing finish.", price: 95, image: hotSencha as string, category: "Drinks" },
  { id: 10, name: "Japanese Corn Soup", description: "Creamy sweet corn potage with a silky texture, Japanese-style.", price: 115, image: cornSoup as string, category: "Soups" },
  { id: 11, name: "Pork Chashu", description: "Rich, melt-in-your-mouth pork belly marinated in a savory-sweet glaze.", price: 130, image: porkCashu2 as string, category: "Mains", tag: "New" },
  { id: 12, name: "Kaisendon", description: "Premium seafood rice bowl with assorted sashimi over seasoned sushi rice.", price: 345, image: kaisendon as string, category: "Mains", tag: "Best Seller" },
  { id: 13, name: "Katsudon", description: "Crispy breaded pork cutlet simmered with egg and onion over fluffy rice.", price: 265, image: katsudon as string, category: "Mains", tag: "Popular" },
  { id: 14, name: "Kabocha Soup", description: "Velvety Japanese pumpkin soup with a hint of ginger and coconut milk.", price: 125, image: kabochaS as string, category: "Soups", tag: "New" },
  { id: 15, name: "Mugi-cha", description: "Cold-brewed roasted barley tea — earthy, smooth, and caffeine-free.", price: 80, image: mugicha as string, category: "Drinks" },
  { id: 16, name: "Kakigori", description: "Japanese shaved ice topped with sweet syrup, condensed milk, and mochi.", price: 145, image: kakigori as string, category: "Desserts", tag: "Seasonal" },
  { id: 17, name: "Oyakodon", description: "Tender chicken and silky egg simmered in dashi sauce over steamed rice.", price: 235, image: oyakodon as string, category: "Mains" },
  { id: 18, name: "Mochi", description: "Handcrafted ice cream mochi in assorted flavors — matcha, ube, and strawberry.", price: 110, image: mochi as string, category: "Desserts", tag: "New" },
  { id: 19, name: "Miso Soup", description: "Classic dashi-based miso broth with tofu, wakame seaweed, and spring onion.", price: 85, image: misoSoup as string, category: "Soups" },
  { id: 20, name: "Pan-Fried Edamame", description: "Wok-tossed edamame with garlic, sesame oil, and sea salt — perfect starter.", price: 120, image: edamame as string, category: "Sides", tag: "Popular" },
  { id: 21, name: "Ramune", description: "Classic Japanese marble soda in original, strawberry, and melon flavors.", price: 90, image: ramune as string, category: "Drinks", tag: "New" },
  { id: 22, name: "Shredded Nori", description: "Toasted and lightly salted shredded seaweed — a savory umami topping.", price: 45, image: shreddedNori as string, category: "Add-ons" },
  { id: 23, name: "Unagi Don", description: "Glazed freshwater eel over steamed rice with a rich kabayaki sauce.", price: 385, image: unagiDon as string, category: "Mains", tag: "Best Seller" },
  { id: 24, name: "Uji Matcha Latte", description: "Premium ceremonial-grade matcha from Uji, Japan, steamed with fresh milk.", price: 155, image: ujiMatcha as string, category: "Drinks", tag: "Popular" },
  { id: 25, name: "Steamed Rice", description: "Fluffy short-grain Japanese rice, steamed to perfection.", price: 40, image: steamedRice as string, category: "Sides" },
  { id: 26, name: "Tendon", description: "Crispy tempura shrimp and vegetables over steamed rice with sweet tsuyu.", price: 295, image: tendon as string, category: "Mains", tag: "New" },
  { id: 27, name: "Yuzu Tea", description: "Warm honey-sweetened yuzu citrus tea — fragrant, soothing, and refreshing.", price: 110, image: yuzuTea as string, category: "Drinks" },
  { id: 28, name: "Yuzu Sparkler", description: "Chilled sparkling yuzu lemonade with a citrus-forward fizz.", price: 125, image: yuzuSparkler as string, category: "Drinks", tag: "New" },
  { id: 29, name: "Kyuri Tsukemono", description: "Lightly pickled Japanese cucumbers with a refreshing crunch.", price: 120, image: kyuriTsukemono as string, category: "Sides" },
  { id: 30, name: "Chicken Yakitori", description: "Grilled chicken skewers glazed with sweet and savory tare sauce.", price: 190, image: yakitoriChicken as string, category: "Sides", tag: "Popular" },
  { id: 31, name: "Spicy Chili Rayu", description: "House-made Japanese chili oil with garlic and sesame.", price: 25, image: spicyChiliRayu as string, category: "Add-ons" },
  { id: 32, name: "Ramyon", description: "Spicy Korean-style noodle soup packed with flavor and warmth.", price: 100, image: ramyon as string, category: "Soups", tag: "New" },
];

const CATEGORIES = ["All", "Mains", "Soups", "Sides", "Add-ons", "Desserts", "Drinks"];

export default function App() {
  const [menuItems, setMenuItems] = useState<MenuEntry[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch("http://localhost:8081/api/menu")
      .then(res => res.json())
      .then(data => {
        const merged = data.map((item: any) => {
          const staticMatch = STATIC_MENU.find(s => s.name === item.itemName) || {} as Partial<MenuEntry>;
          return {
            id: item.id,
            name: item.itemName,
            description: staticMatch.description || "",
            price: item.price,
            image: staticMatch.image || "",
            category: item.category,
            tag: staticMatch.tag
          };
        });
        setMenuItems(merged);
        setLoading(false);
      })
      .catch(err => {
        console.error("Failed to fetch menu:", err);
        setMenuItems(STATIC_MENU);
        setLoading(false);
      });
  }, []);

  const topDishes = menuItems.filter((m) => m.tag === "Best Seller" || m.tag === "Popular").slice(0, 6);
  const featuredHero = menuItems.find(m => m.name === "Unagi Don") || STATIC_MENU[22];

  const [cartOpen, setCartOpen] = useState(false);
  const [cartItems, setCartItems] = useState<CartItem[]>([]);
  const [activeCategory, setActiveCategory] = useState("All");
  const [checkoutOpen, setCheckoutOpen] = useState(false);
  const [loginOpen, setLoginOpen] = useState(false);
  const [receiptOpen, setReceiptOpen] = useState(false);
  const [historyOpen, setHistoryOpen] = useState(false);
  const [currentReceipt, setCurrentReceipt] = useState<OrderReceipt | null>(null);
  const [user, setUser] = useState<{ name: string; email: string } | null>(null);

  const totalQty = cartItems.reduce((s, i) => s + i.qty, 0);

  const addToCart = (id: number) => {
    const item = menuItems.find((m) => m.id === id)!;
    setCartItems((prev) => {
      const existing = prev.find((i) => i.id === id);
      if (existing) return prev.map((i) => (i.id === id ? { ...i, qty: i.qty + 1 } : i));
      return [...prev, { id, name: item.name, price: item.price, qty: 1, image: item.image }];
    });
  };

  const removeFromCart = (id: number) => {
    setCartItems((prev) => prev.map((i) => (i.id === id ? { ...i, qty: i.qty - 1 } : i)).filter((i) => i.qty > 0));
  };

  const deleteFromCart = (id: number) => setCartItems((prev) => prev.filter((i) => i.id !== id));

  const handleCheckout = () => {
    if (!user) { setCartOpen(false); setLoginOpen(true); return; }
    setCartOpen(false);
    setCheckoutOpen(true);
  };

  const filtered = activeCategory === "All" ? menuItems : menuItems.filter((m) => m.category === activeCategory);

  if (loading) {
    return <div className="min-h-screen flex items-center justify-center" style={{ background: "#111110", color: "#c8932a" }}>Loading Menu...</div>;
  }

  return (
    <div className="min-h-screen" style={{ background: "#111110", fontFamily: "'Inter', sans-serif" }}>
      {/* Nav */}
      <nav
        className="fixed top-0 left-0 right-0 z-40 flex items-center justify-between px-6 md:px-12 py-4"
        style={{ background: "rgba(17,17,16,0.93)", backdropFilter: "blur(12px)", borderBottom: "1px solid rgba(200,147,42,0.15)" }}
      >
        <div>
          <p style={{ fontFamily: "'Oswald', sans-serif", color: "#c8932a", fontSize: "1.4rem", letterSpacing: "0.08em", lineHeight: 1 }}>GARAHE NI</p>
          <p style={{ fontFamily: "'Oswald', sans-serif", color: "#f0ede8", fontSize: "0.85rem", letterSpacing: "0.25em", lineHeight: 1 }}>MATEICLA</p>
        </div>

        <div className="hidden md:flex items-center gap-8">
          {["Menu", "About", "Contact"].map((link) => (
            <a key={link} href="#" style={{ color: "#8a8070", fontFamily: "'Inter', sans-serif", fontSize: "0.85rem", letterSpacing: "0.06em" }} className="hover:text-foreground transition-colors">
              {link}
            </a>
          ))}
        </div>

        <div className="flex items-center gap-3">
          {user ? (
            <div className="hidden md:flex items-center gap-3">
              <div className="flex items-center gap-2 px-3 py-2 rounded-lg" style={{ background: "#242320" }}>
                <User size={14} style={{ color: "#c8932a" }} />
                <span style={{ color: "#f0ede8", fontFamily: "'Oswald', sans-serif", fontSize: "0.85rem", letterSpacing: "0.04em" }}>{user.name}</span>
              </div>
              <button onClick={() => setHistoryOpen(true)} className="flex items-center gap-2 px-3 py-2 rounded-lg transition-colors hover:bg-secondary" style={{ color: "#8a8070", fontSize: "0.85rem", fontFamily: "'Oswald', sans-serif" }}>
                MY ORDERS
              </button>
              <button onClick={() => setUser(null)} className="p-2 rounded-lg transition-colors hover:bg-secondary" style={{ color: "#8a8070" }}>
                <LogOut size={16} />
              </button>
            </div>
          ) : (
            <button
              onClick={() => setLoginOpen(true)}
              className="hidden md:flex items-center gap-2 px-4 py-2 rounded-lg transition-all hover:brightness-110"
              style={{ background: "#242320", color: "#c8932a", border: "1px solid rgba(200,147,42,0.3)", fontFamily: "'Oswald', sans-serif", letterSpacing: "0.05em", fontSize: "0.85rem" }}
            >
              <User size={15} /> LOG IN
            </button>
          )}
          <button
            onClick={() => setCartOpen(true)}
            className="relative flex items-center gap-2 px-4 py-2 rounded-lg transition-all hover:brightness-110"
            style={{ background: "#c8932a", color: "#111110" }}
          >
            <ShoppingBag size={18} />
            <span style={{ fontFamily: "'Oswald', sans-serif", letterSpacing: "0.05em", fontSize: "0.9rem" }}>ORDER</span>
            {totalQty > 0 && (
              <span className="absolute -top-2 -right-2 w-5 h-5 rounded-full flex items-center justify-center" style={{ background: "#f0ede8", color: "#111110", fontFamily: "'Oswald', sans-serif", fontSize: "0.7rem" }}>
                {totalQty}
              </span>
            )}
          </button>
        </div>
      </nav>

      {/* Hero */}
      <section className="relative overflow-hidden flex items-end" style={{ height: "90vh", paddingTop: "64px" }}>
        <div className="absolute inset-0">
          <img src={featuredHero.image} alt="Kaisendon hero" className="w-full h-full object-cover" />
          <div className="absolute inset-0" style={{ background: "linear-gradient(to right, rgba(17,17,16,0.93) 40%, rgba(17,17,16,0.3) 100%)" }} />
          <div className="absolute inset-0" style={{ background: "linear-gradient(to top, rgba(17,17,16,1) 0%, transparent 60%)" }} />
        </div>
        <div className="relative z-10 px-6 md:px-16 pb-16 max-w-2xl">
          <div className="flex items-center gap-2 mb-4">
            <div className="w-8 h-px" style={{ background: "#c8932a" }} />
            <span style={{ color: "#c8932a", fontFamily: "'Inter', sans-serif", fontSize: "0.75rem", letterSpacing: "0.18em" }}>AUTHENTIC JAPANESE CUISINE</span>
          </div>
          <h1 style={{ fontFamily: "'Oswald', sans-serif", fontSize: "clamp(3rem, 8vw, 5.5rem)", color: "#f0ede8", letterSpacing: "0.02em", lineHeight: 1, marginBottom: "1.5rem" }}>
            GARAHE NI<br /><span style={{ color: "#c8932a" }}>MATEICLA</span>
          </h1>
          <p style={{ color: "#8a8070", fontSize: "1rem", lineHeight: 1.7, maxWidth: "480px", marginBottom: "2.5rem" }}>
            Where the garage meets the kitchen. Bold flavors, honest food — crafted with Filipino heart and Japanese soul.
          </p>
          <div className="flex flex-wrap gap-3">
            <button
              onClick={() => document.getElementById("menu")?.scrollIntoView({ behavior: "smooth" })}
              className="flex items-center gap-2 px-6 py-3 rounded-lg transition-all hover:brightness-110 active:scale-[0.98]"
              style={{ background: "#c8932a", color: "#111110", fontFamily: "'Oswald', sans-serif", letterSpacing: "0.08em" }}
            >
              VIEW MENU
            </button>
            <div className="flex items-center gap-2 px-4 py-3" style={{ color: "#8a8070", fontSize: "0.85rem" }}>
              <Star size={14} style={{ color: "#c8932a", fill: "#c8932a" }} />
              <span>4.8 · 200+ Reviews</span>
            </div>
          </div>
        </div>
        <div className="absolute bottom-6 left-1/2 -translate-x-1/2 flex flex-col items-center gap-1" style={{ color: "#8a8070" }}>
          <ChevronDown size={20} className="animate-bounce" />
        </div>
      </section>

      {/* Info Bar */}
      <div className="py-4 px-6 md:px-16 flex flex-wrap gap-6 justify-center md:justify-start border-b" style={{ borderColor: "rgba(200,147,42,0.15)", background: "#1c1b19" }}>
        {[
          { icon: <MapPin size={14} />, text: "Mateicla, Philippines" },
          { icon: <Clock size={14} />, text: "Open 10AM – 10PM Daily" },
          { icon: <Phone size={14} />, text: "+63 912 345 6789" },
        ].map(({ icon, text }) => (
          <div key={text} className="flex items-center gap-2" style={{ color: "#8a8070", fontSize: "0.82rem" }}>
            <span style={{ color: "#c8932a" }}>{icon}</span>
            {text}
          </div>
        ))}
      </div>

      {/* Top Dishes */}
      <section className="px-6 md:px-16 py-16">
        <div className="flex items-center gap-4 mb-10">
          <div className="w-1 h-8 rounded-full" style={{ background: "#c8932a" }} />
          <h2 style={{ fontFamily: "'Oswald', sans-serif", color: "#f0ede8", fontSize: "2rem", letterSpacing: "0.06em" }}>TOP DISHES</h2>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {topDishes.map((item) => (
            <MenuItem key={item.id} {...item} onAdd={addToCart} />
          ))}
        </div>
      </section>

      {/* Full Menu */}
      <section id="menu" className="px-6 md:px-16 py-16" style={{ borderTop: "1px solid rgba(200,147,42,0.15)" }}>
        <div className="flex items-center gap-4 mb-8">
          <div className="w-1 h-8 rounded-full" style={{ background: "#c8932a" }} />
          <h2 style={{ fontFamily: "'Oswald', sans-serif", color: "#f0ede8", fontSize: "2rem", letterSpacing: "0.06em" }}>FULL MENU</h2>
        </div>

        {/* Category tabs */}
        <div className="flex gap-2 flex-wrap mb-10">
          {CATEGORIES.map((cat) => (
            <button
              key={cat}
              onClick={() => setActiveCategory(cat)}
              className="px-4 py-2 rounded-lg text-sm transition-all"
              style={{
                fontFamily: "'Oswald', sans-serif",
                letterSpacing: "0.06em",
                background: activeCategory === cat ? "#c8932a" : "#1c1b19",
                color: activeCategory === cat ? "#111110" : "#8a8070",
                border: `1px solid ${activeCategory === cat ? "#c8932a" : "rgba(200,147,42,0.15)"}`,
              }}
            >
              {cat}
            </button>
          ))}
        </div>

        <motion.div
          key={activeCategory}
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25 }}
          className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6"
        >
          {filtered.map((item) => (
            <MenuItem key={item.id} {...item} onAdd={addToCart} />
          ))}
        </motion.div>
      </section>

      {/* Footer */}
      <footer className="py-10 px-6 md:px-16 border-t" style={{ borderColor: "rgba(200,147,42,0.15)", background: "#1c1b19" }}>
        <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
          <div>
            <p style={{ fontFamily: "'Oswald', sans-serif", color: "#c8932a", fontSize: "1.2rem", letterSpacing: "0.08em" }}>GARAHE NI MATEICLA</p>
            <p style={{ color: "#8a8070", fontSize: "0.8rem", marginTop: "4px" }}>Authentic Japanese cuisine with a Filipino soul.</p>
          </div>
          <p style={{ color: "#8a8070", fontSize: "0.75rem" }}>© 2026 Garahe Ni Mateicla. All rights reserved.</p>
        </div>
      </footer>

      {/* Cart overlay */}
      <AnimatePresence>
        {cartOpen && (
          <>
            <motion.div
              initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
              className="fixed inset-0 z-40"
              style={{ background: "rgba(17,17,16,0.6)", backdropFilter: "blur(4px)" }}
              onClick={() => setCartOpen(false)}
            />
            <Cart
              items={cartItems}
              onClose={() => setCartOpen(false)}
              onAdd={addToCart}
              onRemove={removeFromCart}
              onDelete={deleteFromCart}
              onCheckout={handleCheckout}
            />
          </>
        )}
      </AnimatePresence>

      {/* Checkout */}
      <AnimatePresence>
        {checkoutOpen && (
          <CheckoutModal
            items={cartItems}
            onClose={() => { setCheckoutOpen(false); if (currentReceipt) setReceiptOpen(true); }}
            onSuccess={(receipt) => { setCurrentReceipt(receipt); setCartItems([]); }}
            user={user}
          />
        )}
      </AnimatePresence>

      <AnimatePresence>
        {receiptOpen && currentReceipt && (
          <ReceiptModal receipt={currentReceipt} onClose={() => setReceiptOpen(false)} />
        )}
      </AnimatePresence>

      {/* Login */}
      <AnimatePresence>
        {loginOpen && (
          <LoginModal
            onClose={() => setLoginOpen(false)}
            onLogin={(u) => { setUser(u); setLoginOpen(false); }}
          />
        )}
      </AnimatePresence>

      {/* History Modal */}
      <AnimatePresence>
        {historyOpen && user && (
          <TransactionHistoryModal
            user={user}
            onClose={() => setHistoryOpen(false)}
          />
        )}
      </AnimatePresence>

      {/* Floating cart button (mobile) */}
      {totalQty > 0 && !cartOpen && (
        <motion.button
          initial={{ scale: 0 }} animate={{ scale: 1 }}
          onClick={() => setCartOpen(true)}
          className="fixed bottom-6 right-6 z-30 flex items-center gap-2 px-5 py-3 rounded-full shadow-xl md:hidden"
          style={{ background: "#c8932a", color: "#111110" }}
        >
          <ShoppingBag size={18} />
          <span style={{ fontFamily: "'Oswald', sans-serif", letterSpacing: "0.05em" }}>
            {totalQty} item{totalQty > 1 ? "s" : ""}
          </span>
        </motion.button>
      )}
    </div>
  );
}
