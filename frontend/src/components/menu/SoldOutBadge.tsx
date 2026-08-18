import { Badge } from "@/components/ui/Badge";

export function SoldOutBadge({ className }: { className?: string }) {
  return (
    <Badge variant="danger" className={className}>
      품절
    </Badge>
  );
}
