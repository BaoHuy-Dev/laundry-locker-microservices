# Project Artifacts

Thu muc nay gom cac tai lieu, ket qua chay, anh smoke-test va script tien ich duoc copy tu root workspace
`G:\BigProject` vao backend.

> Luu y: cac file goc o root workspace chua bi xoa hoac di chuyen. Thu muc nay la ban gom lai de backend co du bo tai
> lieu ban giao va artifact kiem thu.
> Cac file moi truong/secret/local note nhu `env.txt`, `pro.txt`, `Application.txt` va file host rieng khong duoc copy
> vao day.

## Cau truc

- `guides/`: tai lieu huong dan, handoff, plan va run result dang Markdown.
- `screenshots/`: anh emulator/smoke-test va cac anh ket qua.
- `logs/`: log chay dev/server.
- `scripts/`: script PowerShell tien ich dung de chay/dung/seed he thong.
- `markdown-by-project/`: cac file Markdown cua tung project con, duoc copy va phan loai theo nguon.

## File quan trong

- `../PROJECT_PROGRESS_TRACKER.md`: file theo doi tien do song; bat buoc cap nhat sau moi tinh nang/fix/verification.
- `../BUSINESS_FLOWS_CURRENT.md`: file mo ta chi tiet luong nghiep vu hien tai; bat buoc cap nhat khi behavior thay doi.
- `../CURRENT_PROJECT_STATUS.md`: nguon trang thai chuan hien tai cua du an.
- `guides/RUN_RESULT.md`: ket qua chay he thong va nhat ky thay doi moi nhat.
- `guides/HANDOFF_CODEX.md`: handoff tu phien truoc.
- `guides/LOCKER_FLOW_PLAN.md`: ke hoach va trang thai cac phase luong tu.
- `guides/GAP_ANALYSIS_AND_PLAN.md`: phan tich gap va ke hoach tong the.
- `scripts/run-all.ps1`: build/chay backend va frontend tu root workspace.
- `scripts/stop-all.ps1`: dung he thong.
- `scripts/seed-test-data.ps1`: seed/test data tien ich.
- `markdown-by-project/README.md`: muc luc va thong ke cac file Markdown da gom theo project.

## Cap nhat

Khi tai root workspace co them tai lieu hoac anh moi, copy vao dung thu muc con tuong ung trong
`docs/project-artifacts/`. Khong copy file chua secret/local environment.
