-- Cria o usuário padrão que o sistema vai usar agora que não tem login
INSERT INTO tb_users (id, email, is_verified, password, phone_number, verification_code) VALUES (1, 'user@test.com', true, '123456', '123456789', '0000');

-- Opcional: Se o seu sistema precisar de um perfil vinculado
INSERT INTO tb_user_profile (id, user_id) VALUES (1, 1);