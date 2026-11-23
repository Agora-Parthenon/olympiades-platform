-- Avatar: the id of one of the predefined avatars, optional.
ALTER TABLE profiles ADD COLUMN avatar VARCHAR(30);

-- "Lucy" and "lucy" must not be two different players: uniqueness ignores the case.
CREATE UNIQUE INDEX profiles_pseudo_lower_idx ON profiles (lower(pseudo));
