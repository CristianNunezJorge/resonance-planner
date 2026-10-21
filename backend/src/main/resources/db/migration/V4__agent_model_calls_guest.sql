-- The calls of the guests of "Prova la demo" (D134): all the guests together have a share of the calls of the day, so
-- the registered users always keep the rest. The flag is written with the row, from the role of the user, and stays
-- when the cleanup deletes the guest: like the calls, it must still count until the day is over (no foreign key, V3).
ALTER TABLE agent_model_calls ADD COLUMN guest boolean NOT NULL DEFAULT false;
