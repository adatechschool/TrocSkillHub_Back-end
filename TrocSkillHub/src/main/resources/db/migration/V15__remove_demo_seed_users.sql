-- Remove demo accounts left by older V2/V3 seeds (child rows cascade).
DELETE FROM users
WHERE email IN (
    'jean.martin@example.fr',
    'sophie.lefebvre@example.fr',
    'ada.lovelace@example.fr'
);
