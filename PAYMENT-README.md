# Payment

ส่วน Payment ยังต้องเชื่อมกับ Assessor และ Main ครับ

- Main สร้าง `PaymentGateway(name, dailyTokens)` และเรียก `resetDailyTokens()` ตอนเริ่มวัน
- Assessor เรียก `issuePayments(pendingPayments)` แล้วใช้ `getPaid()` กับ `getRemainingTokens()` ดูผล
- ใช้ `PaymentSummary.sorted(...)` เรียงยอดตอนสรุปครับ
