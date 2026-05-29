# Black-Box User Testing — Inventory Control RFID System (ver1)

> **Sistem:** Inventory Control RFID  
> **Branch:** ver1  
> **Tipe Pengujian:** Black-Box Testing  
> **Tanggal:** 2026-05-29

---

## Kolom Tabel

| Kolom | Keterangan |
|---|---|
| **No** | Nomor urut kasus uji |
| **Deskripsi Fungsional** | Fungsi/fitur yang diuji |
| **Kelompok Uji** | Data Benar / Data Salah |
| **Prosedur & Kasus Uji** | Langkah-langkah pengujian |
| **Hasil yang Diharapkan** | Output/perilaku sistem yang diharapkan |

---

## 1. FITUR: LOGIN & AUTENTIKASI

| No | Deskripsi Fungsional | Kelompok Uji | Prosedur & Kasus Uji | Hasil yang Diharapkan |
|---|---|---|---|---|
| 1.1 | Login dengan credential valid | Data Benar | 1. Buka halaman Login<br>2. Input Username = `"admin"`<br>3. Input Password = `"admin123"`<br>4. Klik tombol **Login** | Berhasil login dan diarahkan ke halaman Dashboard |
| 1.2 | Login dengan username salah | Data Salah | 1. Buka halaman Login<br>2. Input Username = `"adminXXX"` (tidak terdaftar)<br>3. Input Password = `"admin123"`<br>4. Klik tombol **Login** | Gagal login, muncul pesan error *"Username atau password salah"*, tetap di halaman Login |
| 1.3 | Login dengan password salah | Data Salah | 1. Buka halaman Login<br>2. Input Username = `"admin"`<br>3. Input Password = `"wrongpass"`<br>4. Klik tombol **Login** | Gagal login, muncul pesan error *"Username atau password salah"*, tetap di halaman Login |
| 1.4 | Login dengan field kosong | Data Salah | 1. Buka halaman Login<br>2. Biarkan Username kosong<br>3. Biarkan Password kosong<br>4. Klik tombol **Login** | Sistem menampilkan validasi field wajib diisi / gagal login |
| 1.5 | Login dengan username kosong | Data Salah | 1. Buka halaman Login<br>2. Biarkan Username kosong<br>3. Input Password = `"admin123"`<br>4. Klik tombol **Login** | Muncul validasi bahwa Username wajib diisi |
| 1.6 | Login dengan password kosong | Data Salah | 1. Buka halaman Login<br>2. Input Username = `"admin"`<br>3. Biarkan Password kosong<br>4. Klik tombol **Login** | Muncul validasi bahwa Password wajib diisi |
| 1.7 | Logout dari sistem | Data Benar | 1. Login terlebih dahulu<br>2. Klik tombol **Logout** | Sesi dihapus, diarahkan kembali ke halaman Login |
| 1.8 | Akses halaman tanpa login | Data Salah | 1. Tanpa login, buka langsung URL `/dashboard`<br>2. Atau URL modul manapun | Diarahkan otomatis ke halaman Login (redirect) |
| 1.9 | Login API handheld dengan credential valid | Data Benar | 1. Kirim `POST /LoginHT` dengan body `{ "Username": "admin", "Password": "admin123" }` | Response 200, mengembalikan JWT token, username, dan roles |
| 1.10 | Login API handheld dengan credential salah | Data Salah | 1. Kirim `POST /LoginHT` dengan body `{ "Username": "admin", "Password": "salah" }` | Response 400 Bad Request dengan pesan error |

---

## 2. FITUR: MANAJEMEN USER

| No | Deskripsi Fungsional | Kelompok Uji | Prosedur & Kasus Uji | Hasil yang Diharapkan |
|---|---|---|---|---|
| 2.1 | Melihat daftar user | Data Benar | 1. Login sebagai user dengan permission `USER_GET`<br>2. Buka menu **User Management** | Menampilkan daftar semua user yang terdaftar di sistem |
| 2.2 | Membuat user baru dengan data lengkap | Data Benar | 1. Login dengan permission `USER_CREATE`<br>2. Klik tombol **Tambah User**<br>3. Input Fullname = `"John Doe"`<br>4. Input Username = `"johndoe"`<br>5. Input Password = `"pass1234"`<br>6. Pilih Role yang ada<br>7. Klik **Simpan** | User berhasil dibuat, muncul pesan *"User berhasil dibuat"*, user tampil di daftar |
| 2.3 | Membuat user dengan username duplikat | Data Salah | 1. Login dengan permission `USER_CREATE`<br>2. Buat user dengan Username yang sudah ada, misal `"admin"`<br>3. Klik **Simpan** | Gagal simpan, muncul pesan error username sudah digunakan |
| 2.4 | Membuat user dengan field wajib kosong | Data Salah | 1. Login dengan permission `USER_CREATE`<br>2. Klik **Tambah User**<br>3. Biarkan Fullname kosong<br>4. Biarkan Username kosong<br>5. Biarkan Password kosong<br>6. Klik **Simpan** | Muncul validasi field wajib diisi, user tidak tersimpan |
| 2.5 | Membuat user tanpa password | Data Salah | 1. Login dengan permission `USER_CREATE`<br>2. Isi Fullname dan Username<br>3. Biarkan Password kosong<br>4. Klik **Simpan** | Muncul validasi bahwa Password wajib diisi |
| 2.6 | Edit data user (Fullname & Username) | Data Benar | 1. Login dengan permission `USER_UPDATE`<br>2. Pilih user yang ada<br>3. Ubah Fullname = `"Jane Doe"`<br>4. Ubah Username = `"janedoe"`<br>5. Klik **Simpan** | Data user berhasil diperbarui, muncul pesan *"User berhasil diperbarui"* |
| 2.7 | Edit user dengan username duplikat | Data Salah | 1. Login dengan permission `USER_UPDATE`<br>2. Edit user, ubah Username menjadi username milik user lain<br>3. Klik **Simpan** | Gagal update, muncul pesan error username sudah digunakan |
| 2.8 | Ubah password user | Data Benar | 1. Login dengan permission `USER_UPDATE_PASSWORD`<br>2. Pilih user<br>3. Masukkan password baru = `"newpass456"`<br>4. Klik **Simpan** | Password berhasil diubah, muncul pesan *"Password updated successfully"* |
| 2.9 | Ubah password user dengan field kosong | Data Salah | 1. Login dengan permission `USER_UPDATE_PASSWORD`<br>2. Pilih user<br>3. Biarkan field password baru kosong<br>4. Klik **Simpan** | Muncul validasi password wajib diisi |
| 2.10 | Ubah role user | Data Benar | 1. Login dengan permission `USER_UPDATE_ROLE`<br>2. Pilih user<br>3. Ubah role ke role lain yang tersedia<br>4. Klik **Simpan** | Role berhasil diupdate, muncul pesan *"Role berhasil diupdate"* |
| 2.11 | Hapus user | Data Benar | 1. Login dengan permission `USER_DELETE`<br>2. Pilih user yang akan dihapus<br>3. Klik **Hapus** dan konfirmasi | User berhasil dihapus (soft delete), muncul pesan *"User berhasil dihapus"*, user tidak tampil di daftar |
| 2.12 | Hapus user dengan ID tidak valid | Data Salah | 1. Login dengan permission `USER_DELETE`<br>2. Kirim request DELETE dengan ID user yang tidak ada | Muncul error 404 Not Found |
| 2.13 | Akses menu User Management tanpa permission | Data Salah | 1. Login sebagai user tanpa permission `USER_GET`<br>2. Coba akses menu User Management | Akses ditolak / muncul halaman Unauthorized |

---

## 3. FITUR: MANAJEMEN ITEM (MASTER BARANG)

| No | Deskripsi Fungsional | Kelompok Uji | Prosedur & Kasus Uji | Hasil yang Diharapkan |
|---|---|---|---|---|
| 3.1 | Melihat daftar item | Data Benar | 1. Login dengan permission `ITEM_GET`<br>2. Buka menu **Item** | Menampilkan daftar semua item/barang yang tersedia |
| 3.2 | Tambah item baru dengan data lengkap | Data Benar | 1. Login dengan permission `ITEM_CREATE`<br>2. Klik **Tambah Item**<br>3. Input Item Name = `"Laptop ASUS"`<br>4. Input Description = `"Laptop untuk kantor"`<br>5. Klik **Simpan** | Item berhasil dibuat, muncul pesan *"Item berhasil dibuat"*, tampil di daftar |
| 3.3 | Tambah item dengan nama duplikat | Data Salah | 1. Login dengan permission `ITEM_CREATE`<br>2. Tambah item dengan nama yang sudah ada<br>3. Klik **Simpan** | Gagal, muncul pesan error nama item sudah terdaftar |
| 3.4 | Tambah item dengan Item Name kosong | Data Salah | 1. Login dengan permission `ITEM_CREATE`<br>2. Klik **Tambah Item**<br>3. Biarkan Item Name kosong<br>4. Klik **Simpan** | Muncul validasi field Item Name wajib diisi |
| 3.5 | Edit item | Data Benar | 1. Login dengan permission `ITEM_UPDATE`<br>2. Pilih item yang ada<br>3. Ubah Item Name = `"Laptop ASUS Updated"`<br>4. Klik **Simpan** | Item berhasil diperbarui, muncul pesan *"Item berhasil diperbarui"* |
| 3.6 | Edit item dengan ID tidak valid | Data Salah | 1. Login dengan permission `ITEM_UPDATE`<br>2. Kirim request PUT dengan ID item yang tidak ada | Muncul error 404 Not Found |
| 3.7 | Hapus item | Data Benar | 1. Login dengan permission `ITEM_DELETE`<br>2. Pilih item<br>3. Klik **Hapus** dan konfirmasi | Item berhasil dihapus (soft delete), muncul pesan *"Item berhasil dihapus"* |
| 3.8 | Cek stok tersedia untuk item | Data Benar | 1. Login dengan permission `ITEM_GET`<br>2. Pilih item yang punya stok<br>3. Klik **Cek Stok** atau akses available-stock | Menampilkan jumlah stok yang tersedia untuk item tersebut |
| 3.9 | Akses item saat inventory lock aktif | Data Salah | 1. Aktifkan inventory lock<br>2. Coba tambah / edit / hapus item | Operasi ditolak karena inventory sedang dikunci |

---

## 4. FITUR: MANAJEMEN LOKASI

| No | Deskripsi Fungsional | Kelompok Uji | Prosedur & Kasus Uji | Hasil yang Diharapkan |
|---|---|---|---|---|
| 4.1 | Melihat daftar lokasi | Data Benar | 1. Login dengan permission `LOCATION_GET`<br>2. Buka menu **Lokasi** | Menampilkan semua lokasi gudang/storage yang terdaftar |
| 4.2 | Tambah lokasi baru dengan data lengkap | Data Benar | 1. Login dengan permission `LOCATION_CREATE`<br>2. Klik **Tambah Lokasi**<br>3. Input Loc ID = `"LOC-001"`<br>4. Input Name = `"Rak A1"`<br>5. Input Description = `"Rak penyimpanan zona A"`<br>6. Klik **Simpan** | Lokasi berhasil dibuat, muncul pesan *"Location berhasil dibuat"* |
| 4.3 | Tambah lokasi dengan Loc ID duplikat | Data Salah | 1. Login dengan permission `LOCATION_CREATE`<br>2. Tambah lokasi dengan Loc ID yang sudah ada<br>3. Klik **Simpan** | Gagal, muncul pesan error Loc ID sudah digunakan |
| 4.4 | Tambah lokasi dengan field wajib kosong | Data Salah | 1. Login dengan permission `LOCATION_CREATE`<br>2. Klik **Tambah Lokasi**<br>3. Biarkan Name kosong<br>4. Klik **Simpan** | Muncul validasi field Name wajib diisi |
| 4.5 | Tambah lokasi dengan Description kosong | Data Salah | 1. Login dengan permission `LOCATION_CREATE`<br>2. Isi Loc ID dan Name<br>3. Biarkan Description kosong<br>4. Klik **Simpan** | Muncul validasi field Description wajib diisi |
| 4.6 | Edit lokasi | Data Benar | 1. Login dengan permission `LOCATION_UPDATE`<br>2. Pilih lokasi<br>3. Ubah Name = `"Rak A1 Updated"`<br>4. Klik **Simpan** | Lokasi berhasil diperbarui, muncul pesan *"Location berhasil diubah"* |
| 4.7 | Edit lokasi dengan ID tidak valid | Data Salah | 1. Login dengan permission `LOCATION_UPDATE`<br>2. Kirim request PUT dengan ID lokasi yang tidak ada | Muncul error 404 Not Found |
| 4.8 | Hapus lokasi | Data Benar | 1. Login dengan permission `LOCATION_DELETE`<br>2. Pilih lokasi<br>3. Klik **Hapus** dan konfirmasi | Lokasi berhasil dihapus (soft delete), muncul pesan *"Location berhasil dihapus"* |

---

## 5. FITUR: MANAJEMEN RFID READER

| No | Deskripsi Fungsional | Kelompok Uji | Prosedur & Kasus Uji | Hasil yang Diharapkan |
|---|---|---|---|---|
| 5.1 | Melihat daftar reader | Data Benar | 1. Login dengan permission `READER_GET`<br>2. Buka menu **Reader** | Menampilkan semua RFID reader yang terdaftar beserta status (READY / OFFLINE / IN_USE) |
| 5.2 | Daftarkan reader baru dengan data lengkap | Data Benar | 1. Login dengan permission `READER_CREATE`<br>2. Klik **Tambah Reader**<br>3. Input Reader ID = `"RDR-001"`<br>4. Input Reader Name = `"Reader Zona A"`<br>5. Input IP Address = `"192.168.1.100"`<br>6. Pilih Lokasi yang tersedia<br>7. Klik **Simpan** | Reader berhasil didaftarkan, muncul pesan *"Reader berhasil dibuat"* |
| 5.3 | Daftarkan reader dengan Reader ID duplikat | Data Salah | 1. Login dengan permission `READER_CREATE`<br>2. Tambah reader dengan Reader ID yang sudah ada<br>3. Klik **Simpan** | Gagal, muncul pesan error Reader ID sudah digunakan |
| 5.4 | Daftarkan reader dengan field wajib kosong | Data Salah | 1. Login dengan permission `READER_CREATE`<br>2. Klik **Tambah Reader**<br>3. Biarkan IP Address kosong<br>4. Klik **Simpan** | Muncul validasi field IP Address wajib diisi |
| 5.5 | Daftarkan reader tanpa memilih lokasi | Data Salah | 1. Login dengan permission `READER_CREATE`<br>2. Isi semua field kecuali Lokasi<br>3. Klik **Simpan** | Muncul validasi Lokasi wajib dipilih |
| 5.6 | Edit konfigurasi reader | Data Benar | 1. Login dengan permission `READER_UPDATE`<br>2. Pilih reader<br>3. Ubah IP Address = `"192.168.1.200"`<br>4. Klik **Simpan** | Reader berhasil diperbarui, muncul pesan *"Reader berhasil diupdate"* |
| 5.7 | Edit reader dengan ID tidak valid | Data Salah | 1. Login dengan permission `READER_UPDATE`<br>2. Kirim request PUT dengan ID reader yang tidak ada | Muncul error 404 *"Reader tidak ditemukan"* |
| 5.8 | Hapus reader | Data Benar | 1. Login dengan permission `READER_DELETE`<br>2. Pilih reader<br>3. Klik **Hapus** dan konfirmasi | Reader berhasil dihapus, muncul pesan *"Reader berhasil dihapus"* |

---

## 6. FITUR: MANAJEMEN PERMISSION & ROLE

| No | Deskripsi Fungsional | Kelompok Uji | Prosedur & Kasus Uji | Hasil yang Diharapkan |
|---|---|---|---|---|
| 6.1 | Melihat daftar role dan permission | Data Benar | 1. Login dengan permission `PERMISSION_GET`<br>2. Buka menu **Permission** | Menampilkan semua role beserta daftar permission yang dimiliki |
| 6.2 | Membuat role baru dengan permission lengkap | Data Benar | 1. Login dengan permission `PERMISSION_CREATE`<br>2. Klik **Tambah Role**<br>3. Input Role Code = `"SUPERVISOR"`<br>4. Input Role Name = `"Supervisor Gudang"`<br>5. Pilih permission yang diperlukan (misal: ITEM_GET, STOCK_IN, STOCK_OUT)<br>6. Klik **Simpan** | Role berhasil dibuat, muncul konfirmasi sukses |
| 6.3 | Membuat role dengan Role Code duplikat | Data Salah | 1. Login dengan permission `PERMISSION_CREATE`<br>2. Buat role dengan Role Code yang sudah ada<br>3. Klik **Simpan** | Gagal, muncul pesan error Role Code sudah digunakan |
| 6.4 | Membuat role dengan field wajib kosong | Data Salah | 1. Login dengan permission `PERMISSION_CREATE`<br>2. Biarkan Role Code dan Role Name kosong<br>3. Klik **Simpan** | Muncul validasi field wajib diisi |
| 6.5 | Edit role dan ubah daftar permission | Data Benar | 1. Login dengan permission `PERMISSION_UPDATE`<br>2. Pilih role yang ada<br>3. Tambah / kurangi permission<br>4. Klik **Simpan** | Role dan permission berhasil diperbarui |
| 6.6 | Hapus role | Data Benar | 1. Login dengan permission `PERMISSION_DELETE`<br>2. Pilih role<br>3. Klik **Hapus** dan konfirmasi | Role berhasil dihapus |
| 6.7 | Verifikasi permission berfungsi dengan benar | Data Benar | 1. Buat user dengan role yang hanya punya `ITEM_GET`<br>2. Login sebagai user tersebut<br>3. Coba akses menu Item (hanya lihat)<br>4. Coba klik tombol Tambah Item | Menu Item dapat diakses (read only), tombol Tambah tidak tersedia atau akses ditolak |

---

## 7. FITUR: CETAK & REGISTRASI TAG RFID

| No | Deskripsi Fungsional | Kelompok Uji | Prosedur & Kasus Uji | Hasil yang Diharapkan |
|---|---|---|---|---|
| 7.1 | Cetak tag RFID untuk item | Data Benar | 1. Login dengan permission `TAG_PRINT`<br>2. Buka menu **Print Tag**<br>3. Pilih Item = `"Laptop ASUS"`<br>4. Input Qty = `5`<br>5. Klik **Cetak** | 5 tag berhasil dicetak, sistem mengembalikan batch number cetak |
| 7.2 | Cetak tag dengan Qty = 0 | Data Salah | 1. Login dengan permission `TAG_PRINT`<br>2. Pilih Item<br>3. Input Qty = `0`<br>4. Klik **Cetak** | Muncul validasi Qty harus lebih dari 0 |
| 7.3 | Cetak tag tanpa memilih item | Data Salah | 1. Login dengan permission `TAG_PRINT`<br>2. Tidak pilih item apapun<br>3. Input Qty = `5`<br>4. Klik **Cetak** | Muncul validasi item harus dipilih |
| 7.4 | Cetak banyak tag (bulk print) dengan daftar kosong | Data Salah | 1. Login dengan permission `TAG_PRINT`<br>2. Kirim request print bulk dengan list kosong | Gagal, muncul pesan error *"Data tidak boleh kosong"* |
| 7.5 | Registrasi tag yang sudah dicetak menjadi Standby | Data Benar | 1. Login dengan permission `TAG_REGISTER`<br>2. Pilih tag-tag yang status-nya PRINTED<br>3. Klik **Register / Standby**<br>4. Konfirmasi | Tag berhasil diregistrasi, status berubah menjadi STANDBY, muncul pesan *"Tag berhasil di-standby-kan"* |
| 7.6 | Registrasi tag dengan list tag kosong | Data Salah | 1. Login dengan permission `TAG_REGISTER`<br>2. Tidak pilih tag apapun<br>3. Klik **Register** | Muncul pesan error / validasi, tidak ada tag yang diregistrasi |
| 7.7 | Melihat riwayat cetak tag | Data Benar | 1. Login dengan permission `TAG_GET`<br>2. Buka menu **Tag History** | Menampilkan riwayat tag yang sudah dicetak, termasuk status dan batch number |
| 7.8 | Melihat stok tag per item | Data Benar | 1. Login dengan permission `TAG_GET`<br>2. Akses halaman **Tag Stock** | Menampilkan jumlah stok tag (IN_STOCK) per item |
| 7.9 | Cari tag dengan QR code valid | Data Benar | 1. Login dengan permission `TAG_GET`<br>2. Scan atau input Tag ID / QR code yang valid<br>3. Klik **Cari** | Menampilkan detail tag: TagId, Nama Item, Lokasi, dan Total Stok |
| 7.10 | Cari tag dengan QR code tidak valid | Data Salah | 1. Login dengan permission `TAG_GET`<br>2. Input Tag ID yang tidak ada = `"XXXXXXXX"`<br>3. Klik **Cari** | Muncul error 404 *"Tag not found or already deleted"* |

---

## 8. FITUR: STOCK IN (PENERIMAAN BARANG)

| No | Deskripsi Fungsional | Kelompok Uji | Prosedur & Kasus Uji | Hasil yang Diharapkan |
|---|---|---|---|---|
| 8.1 | Stock In menggunakan RFID dengan data valid | Data Benar | 1. Login dengan permission `STOCK_IN`<br>2. Pilih Scanner Type = `"RFID"`<br>3. Pilih Lokasi tujuan = `"LOC-001"`<br>4. Scan / input EPC tag yang valid: `["EPC0001", "EPC0002"]`<br>5. Klik **Proses Stock In** | Stock In berhasil, status tag berubah menjadi IN_STOCK, lokasi tag diperbarui ke LOC-001, muncul pesan *"Stock In successful"* |
| 8.2 | Stock In menggunakan QR Code dengan data valid | Data Benar | 1. Login dengan permission `STOCK_IN`<br>2. Pilih Scanner Type = `"QR"`<br>3. Pilih Lokasi tujuan<br>4. Input kode QR tag yang valid<br>5. Klik **Proses Stock In** | Stock In berhasil, status tag berubah menjadi IN_STOCK, rekaman transaksi terbentuk |
| 8.3 | Stock In tanpa memilih lokasi tujuan | Data Salah | 1. Login dengan permission `STOCK_IN`<br>2. Pilih Scanner Type<br>3. Tidak pilih Lokasi<br>4. Masukkan kode tag<br>5. Klik **Proses Stock In** | Gagal, muncul validasi Lokasi wajib dipilih |
| 8.4 | Stock In dengan kode tag tidak valid | Data Salah | 1. Login dengan permission `STOCK_IN`<br>2. Pilih Scanner Type = `"RFID"`<br>3. Pilih Lokasi<br>4. Input EPC yang tidak terdaftar = `["EPCINVALID99"]`<br>5. Klik **Proses Stock In** | Muncul error 404, tag tidak ditemukan di sistem |
| 8.5 | Stock In tanpa input kode scan | Data Salah | 1. Login dengan permission `STOCK_IN`<br>2. Pilih Scanner Type dan Lokasi<br>3. Tidak masukkan kode scan apapun<br>4. Klik **Proses Stock In** | Muncul validasi minimal satu kode scan wajib dimasukkan |
| 8.6 | Stock In saat inventory lock aktif | Data Salah | 1. Aktifkan inventory lock<br>2. Login dengan permission `STOCK_IN`<br>3. Coba lakukan Stock In | Operasi ditolak karena inventory sedang dikunci |

---

## 9. FITUR: STOCK OUT (PENGELUARAN BARANG)

| No | Deskripsi Fungsional | Kelompok Uji | Prosedur & Kasus Uji | Hasil yang Diharapkan |
|---|---|---|---|---|
| 9.1 | Mulai sesi scan RFID untuk Stock Out | Data Benar | 1. Login dengan permission `STOCK_OUT`<br>2. Pilih Delivery Order (DO) yang valid<br>3. Pilih Reader ID = `"RDR-001"`<br>4. Masukkan IP Address reader<br>5. Klik **Start Scan** | Reader berhasil terhubung, muncul konfirmasi *"Reader started"*, siap melakukan scan |
| 9.2 | Start scan dengan Reader ID tidak valid | Data Salah | 1. Login dengan permission `STOCK_OUT`<br>2. Pilih DO<br>3. Masukkan Reader ID yang tidak terdaftar<br>4. Klik **Start Scan** | Gagal koneksi reader, muncul pesan error |
| 9.3 | Start scan tanpa memilih DO | Data Salah | 1. Login dengan permission `STOCK_OUT`<br>2. Pilih Reader ID<br>3. Tidak pilih DO apapun<br>4. Klik **Start Scan** | Muncul validasi DO wajib dipilih |
| 9.4 | Scan tag RFID yang sesuai dengan DO | Data Benar | 1. Setelah reader aktif<br>2. Dekatkan tag RFID yang termasuk dalam DO<br>3. Tag terbaca oleh reader | Tag berhasil discan, terbaca sebagai valid, muncul konfirmasi *"Scanned"*, progress scan bertambah |
| 9.5 | Scan tag RFID yang tidak ada di DO | Data Salah | 1. Setelah reader aktif<br>2. Dekatkan tag RFID yang tidak termasuk dalam DO | Tag terdeteksi sebagai invalid, sistem menyimpan tag invalid, muncul notifikasi scan tidak valid |
| 9.6 | Cek progress scan | Data Benar | 1. Setelah beberapa tag discan<br>2. Cek progress DO | Menampilkan berapa tag sudah discan vs total tag yang diperlukan dalam DO |
| 9.7 | Finalisasi Stock Out dengan semua item sudah discan | Data Benar | 1. Setelah semua tag DO discan<br>2. Klik **Finalize Stock Out** | Stock Out berhasil difinalisasi, status tag berubah menjadi OUT, rekaman transaksi terbentuk, muncul pesan *"Stock Out finalized"* |
| 9.8 | Finalisasi Stock Out dengan item belum semua discan | Data Salah | 1. Hanya sebagian tag DO yang discan<br>2. Klik **Finalize Stock Out** | Muncul peringatan/error scan belum selesai, atau sistem menampilkan item yang belum discan |
| 9.9 | Stop reader setelah selesai | Data Benar | 1. Klik **Stop Reader** | Reader berhenti, sesi scan diakhiri, muncul konfirmasi *"Reader stopped"* |

---

## 10. FITUR: STOCK TAKING (OPNAME FISIK)

| No | Deskripsi Fungsional | Kelompok Uji | Prosedur & Kasus Uji | Hasil yang Diharapkan |
|---|---|---|---|---|
| 10.1 | Buat sesi Stock Taking baru | Data Benar | 1. Login dengan permission `STOCK_TAKING_CREATE`<br>2. Buka menu **Stock Taking**<br>3. Input Remark = `"Opname Bulanan Mei 2026"`<br>4. Pilih lokasi yang akan diopname (opsional)<br>5. Klik **Mulai Stock Taking** | Sesi Stock Taking berhasil dibuat, muncul ID sesi baru |
| 10.2 | Buat sesi Stock Taking tanpa remark | Data Benar | 1. Login dengan permission `STOCK_TAKING_CREATE`<br>2. Klik **Mulai Stock Taking** tanpa isi Remark | Sesi Stock Taking berhasil dibuat (Remark bersifat opsional) |
| 10.3 | Cek status sesi aktif | Data Benar | 1. Setelah membuat sesi<br>2. Akses **Active Session** | Menampilkan sesi Stock Taking yang sedang berjalan |
| 10.4 | Scan tag satu per satu | Data Benar | 1. Login dengan permission `STOCK_TAKING_SCAN`<br>2. Input Stt ID yang valid<br>3. Scan EPC tag yang ada di inventori: `"EPC0001"`<br>4. Klik **Scan** | Tag berhasil discan, muncul konfirmasi *"Tag discan"*, progress opname bertambah |
| 10.5 | Scan tag dengan EPC tidak valid | Data Salah | 1. Login dengan permission `STOCK_TAKING_SCAN`<br>2. Input EPC yang tidak terdaftar di sistem<br>3. Klik **Scan** | Muncul error, EPC tidak ditemukan di sistem |
| 10.6 | Bulk scan banyak tag sekaligus | Data Benar | 1. Login dengan permission `STOCK_TAKING_SCAN`<br>2. Kirim request bulk scan dengan list EPC: `["EPC001", "EPC002", "EPC003"]`<br>3. Submit | Semua tag berhasil discan secara bulk |
| 10.7 | Tandai tag sebagai Remove (hilang/rusak) | Data Benar | 1. Login dengan permission `STOCK_TAKING_REMOVE`<br>2. Pilih tag yang tidak ditemukan saat opname<br>3. Klik **Tandai Remove** | Tag berhasil ditandai sebagai removed, muncul pesan *"Tag ditandai remove"* |
| 10.8 | Manual add untuk tag yang tidak ada di sistem | Data Benar | 1. Login dengan permission `STOCK_TAKING_MANUAL`<br>2. Pilih Item yang stoknya tidak tercatat<br>3. Input Tag ID baru = `"TAG-MANUAL01"`<br>4. Isi Remark = `"Tag fisik ada tapi tidak tercatat"`<br>5. Klik **Manual Add** | Manual adjustment berhasil dicatat, muncul pesan *"Manual add dicatat"* |
| 10.9 | Manual add dengan Item ID tidak valid | Data Salah | 1. Login dengan permission `STOCK_TAKING_MANUAL`<br>2. Input Item ID yang tidak ada<br>3. Klik **Manual Add** | Muncul error Item tidak ditemukan |
| 10.10 | Lihat perbandingan sistem vs scan fisik | Data Benar | 1. Setelah scan selesai<br>2. Klik **Compare** atau akses laporan perbandingan | Menampilkan tabel perbandingan: ItemId, QtySystem, QtyScan, Selisih, Status (Matched/Missing/Extra) per item |
| 10.11 | Finalisasi Stock Taking | Data Benar | 1. Login dengan permission `STOCK_TAKING_FINALIZE`<br>2. Setelah proses opname selesai<br>3. Klik **Finalisasi** | Stock Taking selesai, muncul pesan *"Stock Taking selesai"*, sesi tidak aktif lagi |
| 10.12 | Export laporan opname format Excel | Data Benar | 1. Login dengan permission yang sesuai<br>2. Klik **Export Excel** pada halaman Stock Taking | File Excel berhasil diunduh dengan nama format `Export_yyyyMMddHHmmss.xlsx` berisi data stok sistem |
| 10.13 | Export laporan perbandingan format CSV | Data Benar | 1. Login dengan permission yang sesuai<br>2. Klik **Export CSV** pada laporan perbandingan | File CSV berhasil diunduh dengan kolom: ItemId, Location, QtySystem, QtyScan, Difference, Status |
| 10.14 | Cek progress Stock Taking | Data Benar | 1. Selama sesi berjalan<br>2. Klik **Cek Progress** | Menampilkan persentase / jumlah item sudah discan vs total item yang harus dicheck |

---

## 11. FITUR: PICKING LIST / DELIVERY ORDER (DO)

| No | Deskripsi Fungsional | Kelompok Uji | Prosedur & Kasus Uji | Hasil yang Diharapkan |
|---|---|---|---|---|
| 11.1 | Melihat daftar Delivery Order | Data Benar | 1. Login dengan permission `PICKINGLIST_GET`<br>2. Buka menu **Picking List / DO** | Menampilkan semua Delivery Order dengan status masing-masing |
| 11.2 | Buat DO baru dengan detail item | Data Benar | 1. Login dengan permission `PICKINGLIST_CREATE`<br>2. Klik **Buat DO**<br>3. Input DO Number = `"DO-2026-001"`<br>4. Tambah detail: pilih Item = `"Laptop ASUS"`, Qty = `3`<br>5. Klik **Simpan** | DO berhasil dibuat, muncul pesan *"DO berhasil dibuat"*, tampil di daftar |
| 11.3 | Buat DO dengan detail item kosong (tanpa item) | Data Salah | 1. Login dengan permission `PICKINGLIST_CREATE`<br>2. Input DO Number<br>3. Tidak tambahkan item apapun<br>4. Klik **Simpan** | Gagal, muncul pesan *"Detail DO tidak boleh kosong"* |
| 11.4 | Buat DO tanpa DO Number | Data Salah | 1. Login dengan permission `PICKINGLIST_CREATE`<br>2. Biarkan DO Number kosong<br>3. Tambah item detail<br>4. Klik **Simpan** | Muncul validasi field DO Number wajib diisi |
| 11.5 | Edit DO | Data Benar | 1. Login dengan permission `PICKINGLIST_UPDATE`<br>2. Pilih DO yang ada<br>3. Ubah DO Number atau tambah item<br>4. Klik **Simpan** | DO berhasil diperbarui, muncul pesan *"DO berhasil diupdate"* |
| 11.6 | Edit DO dengan ID tidak valid | Data Salah | 1. Login dengan permission `PICKINGLIST_UPDATE`<br>2. Kirim request PUT dengan ID DO yang tidak ada | Muncul error 404 Not Found |
| 11.7 | Hapus DO | Data Benar | 1. Login dengan permission `PICKINGLIST_DELETE`<br>2. Pilih DO<br>3. Klik **Hapus** dan konfirmasi | DO berhasil dihapus (soft delete), muncul pesan *"DO berhasil dihapus"* |
| 11.8 | Lihat detail DO beserta tag yang terAssign | Data Benar | 1. Login dengan permission `PICKINGLIST_GET`<br>2. Klik salah satu DO<br>3. Lihat detail | Menampilkan: DoId, DoNumber, Status, daftar item + qty required, daftar tag (TagId dan EpcTag) |
| 11.9 | Buat DO dengan Item ID tidak valid | Data Salah | 1. Login dengan permission `PICKINGLIST_CREATE`<br>2. Tambah detail dengan Item ID yang tidak ada di sistem<br>3. Klik **Simpan** | Gagal, muncul pesan error Item tidak ditemukan |

---

## 12. FITUR: RIWAYAT TRANSAKSI & LAPORAN

| No | Deskripsi Fungsional | Kelompok Uji | Prosedur & Kasus Uji | Hasil yang Diharapkan |
|---|---|---|---|---|
| 12.1 | Melihat semua riwayat transaksi | Data Benar | 1. Login dengan permission `TRANSACTION_GET`<br>2. Buka menu **Transaction History** | Menampilkan semua riwayat transaksi (Stock In, Stock Out, Stock Taking) |
| 12.2 | Filter transaksi berdasarkan tanggal | Data Benar | 1. Login dengan permission `TRANSACTION_GET`<br>2. Set From Date = `2026-05-01`<br>3. Set To Date = `2026-05-29`<br>4. Klik **Cari** | Menampilkan transaksi dalam rentang tanggal yang dipilih |
| 12.3 | Filter transaksi berdasarkan tipe | Data Benar | 1. Login dengan permission `TRANSACTION_GET`<br>2. Pilih Transaction Type = `"STOCK_IN"`<br>3. Klik **Cari** | Menampilkan hanya transaksi bertipe Stock In |
| 12.4 | Filter transaksi dengan keyword | Data Benar | 1. Login dengan permission `TRANSACTION_GET`<br>2. Input Keyword = `"Laptop"`<br>3. Klik **Cari** | Menampilkan transaksi yang berhubungan dengan keyword tersebut |
| 12.5 | Filter dengan rentang tanggal tidak valid (From > To) | Data Salah | 1. Login dengan permission `TRANSACTION_GET`<br>2. Set From Date = `2026-05-29`<br>3. Set To Date = `2026-05-01` (lebih kecil)<br>4. Klik **Cari** | Muncul pesan error tanggal tidak valid atau hasil kosong |
| 12.6 | Lihat detail satu transaksi | Data Benar | 1. Login dengan permission `TRANSACTION_GET`<br>2. Klik pada salah satu transaksi di daftar | Menampilkan detail lengkap transaksi: item, lokasi, tag yang terlibat, user yang melakukan, waktu |
| 12.7 | Lihat detail transaksi dengan ID tidak valid | Data Salah | 1. Login dengan permission `TRANSACTION_GET`<br>2. Akses detail dengan ID transaksi yang tidak ada | Muncul error 404 Not Found |
| 12.8 | Export transaksi ke Excel | Data Benar | 1. Login dengan permission `TRANSACTION_GET`<br>2. (Opsional) terapkan filter<br>3. Klik **Export Excel** | File Excel berhasil diunduh dengan nama format `Transactions_{dateRange}_{txType}.xlsx` |
| 12.9 | Export transaksi ke CSV | Data Benar | 1. Login dengan permission `TRANSACTION_GET`<br>2. (Opsional) terapkan filter<br>3. Klik **Export CSV** | File CSV berhasil diunduh dengan nama format `Transactions_{dateRange}_{txType}.csv` |
| 12.10 | Export tanpa data transaksi yang cocok | Data Salah | 1. Login dengan permission `TRANSACTION_GET`<br>2. Filter dengan tanggal yang tidak ada datanya<br>3. Klik **Export** | File Excel/CSV terunduh namun berisi header tanpa data, atau muncul pesan tidak ada data |

---

## 13. FITUR: PENCARIAN ITEM / TAG

| No | Deskripsi Fungsional | Kelompok Uji | Prosedur & Kasus Uji | Hasil yang Diharapkan |
|---|---|---|---|---|
| 13.1 | Lihat semua item | Data Benar | 1. Login dengan permission yang sesuai<br>2. Buka menu **Search Item** | Menampilkan daftar semua item beserta informasi stok |
| 13.2 | Cari item berdasarkan kode tag yang valid | Data Benar | 1. Login dengan permission yang sesuai<br>2. Input kode tag yang valid di kolom pencarian<br>3. Klik **Cari** | Menampilkan detail: TagId, Nama Item, Lokasi, Total Stok |
| 13.3 | Cari item dengan kode tag tidak valid | Data Salah | 1. Login dengan permission yang sesuai<br>2. Input kode tag yang tidak ada = `"XXXNOTEXIST"`<br>3. Klik **Cari** | Muncul error 404 *"Tag not found or already deleted"* |
| 13.4 | Cari item dengan kode yang sudah dihapus | Data Salah | 1. Login dengan permission yang sesuai<br>2. Input kode tag yang sudah di-soft-delete<br>3. Klik **Cari** | Muncul error 404 *"Tag not found or already deleted"* |

---

## 14. FITUR: KONFIGURASI SISTEM

| No | Deskripsi Fungsional | Kelompok Uji | Prosedur & Kasus Uji | Hasil yang Diharapkan |
|---|---|---|---|---|
| 14.1 | Update konfigurasi koneksi database yang valid | Data Benar | 1. Login sebagai admin<br>2. Akses halaman **Konfigurasi**<br>3. Input connection string database yang valid<br>4. Klik **Update** | Konfigurasi berhasil diperbarui, muncul pesan *"Connection updated. Restarting app..."*, aplikasi restart |
| 14.2 | Update konfigurasi dengan connection string kosong | Data Salah | 1. Login sebagai admin<br>2. Akses halaman **Konfigurasi**<br>3. Biarkan field connection string kosong<br>4. Klik **Update** | Muncul validasi field wajib diisi |
| 14.3 | Update konfigurasi dengan connection string tidak valid | Data Salah | 1. Login sebagai admin<br>2. Input connection string yang salah format = `"INVALID_CONN"`<br>3. Klik **Update** | Muncul pesan error koneksi gagal / format tidak valid |

---

## Ringkasan Kasus Uji

| No | Fitur | Jumlah Kasus Uji | Data Benar | Data Salah |
|---|---|---|---|---|
| 1 | Login & Autentikasi | 10 | 4 | 6 |
| 2 | Manajemen User | 13 | 7 | 6 |
| 3 | Manajemen Item | 9 | 5 | 4 |
| 4 | Manajemen Lokasi | 8 | 5 | 3 |
| 5 | Manajemen RFID Reader | 8 | 5 | 3 |
| 6 | Manajemen Permission & Role | 7 | 5 | 2 |
| 7 | Cetak & Registrasi Tag RFID | 10 | 7 | 3 |
| 8 | Stock In | 6 | 3 | 3 |
| 9 | Stock Out | 9 | 5 | 4 |
| 10 | Stock Taking | 14 | 11 | 3 |
| 11 | Picking List / DO | 9 | 5 | 4 |
| 12 | Riwayat Transaksi & Laporan | 10 | 7 | 3 |
| 13 | Pencarian Item / Tag | 4 | 2 | 2 |
| 14 | Konfigurasi Sistem | 3 | 1 | 2 |
| | **TOTAL** | **120** | **72** | **48** |
