"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState
} from "react";

export type CartItem = {
  id: number;
  slug: string;
  name: string;
  price: number;
  imageUrl: string | null;
  quantity: number;
};

type NewCartItem = Omit<CartItem, "quantity">;

type CartContextType = {
  items: CartItem[];
  itemCount: number;
  subtotal: number;
  ready: boolean;
  addItem: (product: NewCartItem) => void;
  updateQuantity: (id: number, quantity: number) => void;
  removeItem: (id: number) => void;
  clearCart: () => void;
};

const STORAGE_KEY = "kcmart_guest_cart_v1";

const CartContext = createContext<CartContextType | null>(null);

export function CartProvider({
  children
}: {
  children: React.ReactNode;
}) {
  const [items, setItems] = useState<CartItem[]>([]);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);

      if (saved) {
        const parsed: unknown = JSON.parse(saved);

        if (Array.isArray(parsed)) {
          const valid = parsed.filter(
            (item): item is CartItem =>
              item !== null &&
              typeof item === "object" &&
              Number.isSafeInteger(item.id) &&
              item.id > 0 &&
              typeof item.slug === "string" &&
              typeof item.name === "string" &&
              typeof item.price === "number" &&
              Number.isFinite(item.price) &&
              item.price >= 0 &&
              (item.imageUrl === null ||
                typeof item.imageUrl === "string") &&
              Number.isInteger(item.quantity) &&
              item.quantity >= 1 &&
              item.quantity <= 10
          );

          setItems(valid);
        }
      }
    } catch {
      setItems([]);
    } finally {
      setReady(true);
    }
  }, []);

  useEffect(() => {
    if (!ready) return;

    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
    } catch {
      // Storage may be unavailable in private browsing.
    }
  }, [items, ready]);

  const addItem = useCallback((product: NewCartItem) => {
    setItems((current) => {
      const existing = current.find(
        (item) => item.id === product.id
      );

      if (existing) {
        return current.map((item) =>
          item.id === product.id
            ? {
                ...item,
                quantity: Math.min(item.quantity + 1, 10)
              }
            : item
        );
      }

      return [...current, { ...product, quantity: 1 }];
    });
  }, []);

  const updateQuantity = useCallback(
    (id: number, quantity: number) => {
      if (!Number.isInteger(quantity)) return;

      setItems((current) =>
        current.map((item) =>
          item.id === id
            ? {
                ...item,
                quantity: Math.max(1, Math.min(10, quantity))
              }
            : item
        )
      );
    },
    []
  );

  const removeItem = useCallback((id: number) => {
    setItems((current) =>
      current.filter((item) => item.id !== id)
    );
  }, []);

  const clearCart = useCallback(() => {
    setItems([]);
  }, []);

  const itemCount = useMemo(
    () => items.reduce(
      (total, item) => total + item.quantity,
      0
    ),
    [items]
  );

  const subtotal = useMemo(
    () => items.reduce(
      (total, item) => total + item.price * item.quantity,
      0
    ),
    [items]
  );

  return (
    <CartContext.Provider
      value={{
        items,
        itemCount,
        subtotal,
        ready,
        addItem,
        updateQuantity,
        removeItem,
        clearCart
      }}
    >
      {children}
    </CartContext.Provider>
  );
}

export function useCart() {
  const context = useContext(CartContext);

  if (!context) {
    throw new Error("useCart must be used inside CartProvider");
  }

  return context;
}