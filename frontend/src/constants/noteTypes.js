export const NOTE_TYPES = [
  { value: "PREDICCION", label: "Predicción" },
  { value: "MOTIVO_COMPRA", label: "Motivo de compra" },
  { value: "MOTIVO_VENTA", label: "Motivo de venta" },
  { value: "OBSERVACION", label: "Observación" },
];

export const NOTE_TYPE_LABELS = Object.fromEntries(
  NOTE_TYPES.map((type) => [type.value, type.label])
);
