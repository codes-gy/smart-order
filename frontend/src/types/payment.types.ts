export type PaymentMethod = "KAKAO_PAY" | "TOSS_PAY" | "CARD";

export interface PaymentMethodOption {
  id: PaymentMethod;
  label: string;
}
