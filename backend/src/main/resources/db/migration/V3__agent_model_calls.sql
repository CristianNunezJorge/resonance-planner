-- The calls to the model of the agent, one row per day and user (D126, D129). The day is in UTC, like the free limit of
-- OpenRouter: the sum of a day is the count of the whole app, the row of a user is that user's share. Kept in the
-- database and not in memory: the free server of Render stops after 15 minutes without requests, and a counter in
-- memory would start again from zero at every restart.
-- No foreign key to users on purpose: the rows of a deleted account stay until the day is over, with only the id and no
-- data of the person. If they went away with the account, the total of the app would go down, and an account that used
-- its calls could be deleted and registered again to use the calls of everyone without end.
CREATE TABLE agent_model_calls (
    call_date date    NOT NULL,
    user_id   bigint  NOT NULL,
    calls     integer NOT NULL,
    CONSTRAINT agent_model_calls_pkey PRIMARY KEY (call_date, user_id),
    CONSTRAINT agent_model_calls_calls_check CHECK (calls IS NOT NULL AND calls >= 0)
);
