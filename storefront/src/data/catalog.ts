export type Category = {
  name: string;
  slug: string;
  group: string;
  icon: string;
};

export type Product = {
  id: number;
  name: string;
  category: string;
  price: number;
  originalPrice: number;
  badge?: string;
  icon: string;
};

const groups = [
  {
    group: "Computers & Components",
    icon: "monitor",
    names: [
      "Desktop", "Laptop", "Motherboard", "RAM",
      "SSD", "HDD", "CPU Fan", "Cabinet", "Monitor"
    ]
  },
  {
    group: "Printers & Consumables",
    icon: "printer",
    names: [
      "Printers", "Cartridge", "Toner Powder",
      "Ink", "Photo Paper", "Printer Accessories"
    ]
  },
  {
    group: "Power & Backup",
    icon: "battery",
    names: [
      "Inverter", "Inverter Battery", "UPS", "Spike"
    ]
  },
  {
    group: "Accessories & Spare Parts",
    icon: "keyboard",
    names: [
      "Keyboard", "Mouse", "Laptop Battery",
      "Laptop Screen", "Laptop Keyboard",
      "Desktop Accessories", "Laptop Accessories"
    ]
  },
  {
    group: "Security & Multimedia",
    icon: "camera",
    names: [
      "CCTV Camera", "Camera Accessories",
      "Woofer", "Finger Scanner"
    ]
  }
];

export const categories: Category[] = groups.flatMap(
  ({ group, icon, names }) =>
    names.map((name) => ({
      name,
      slug: name.toLowerCase().replace(/[^a-z0-9]+/g, "-"),
      group,
      icon
    }))
);

export const featuredCategories = [
  "Laptop",
  "Desktop",
  "Printers",
  "SSD",
  "Monitor",
  "CCTV Camera",
  "UPS",
  "Keyboard"
].map((name) => categories.find((c) => c.name === name)!);

// DEMO DATA ONLY - replace with real inventory from Spring Boot.
export const demoProducts: Product[] = [
  {
    id: 1,
    name: "Business Laptop - 15.6 inch",
    category: "Laptop",
    price: 42999,
    originalPrice: 47999,
    badge: "Demo product",
    icon: "laptop"
  },
  {
    id: 2,
    name: "512GB NVMe Solid State Drive",
    category: "SSD",
    price: 3299,
    originalPrice: 3999,
    icon: "hard-drive"
  },
  {
    id: 3,
    name: "24 inch Full HD LED Monitor",
    category: "Monitor",
    price: 8999,
    originalPrice: 10999,
    icon: "monitor"
  },
  {
    id: 4,
    name: "Wireless Keyboard and Mouse Combo",
    category: "Keyboard",
    price: 1299,
    originalPrice: 1799,
    icon: "keyboard"
  }
];

export const formatINR = (amount: number) =>
  new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0
  }).format(amount);
