# Payment

ส่วน Payment เชื่อมกับ Assessor และ Main ผ่าน API ดังนี้

- Main สร้าง `PaymentGateway(name, dailyTokens)` และเรียก `resetTokens()` ตอนเริ่มวัน
- Assessor เรียก `issuePayments(pendingPayments)` และรับจำนวน payments ที่สำเร็จเป็น `int`; ดู tokens ที่เหลือด้วย `getRemainingTokens()`
- Main เรียง summary จาก successful payments มากไปน้อย และเรียงชื่อ thread เมื่อยอดเท่ากัน
