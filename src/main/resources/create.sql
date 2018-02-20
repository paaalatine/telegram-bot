CREATE SEQUENCE sentence_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE word_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE association_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE chat_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE temp (
    sentence_id integer NOT NULL,
    weight decimal NOT NULL
);

CREATE TABLE sentence (
    id integer PRIMARY KEY DEFAULT nextval('sentence_id_seq'),
    text character varying NOT NULL
);

CREATE TABLE word (
    id integer PRIMARY KEY DEFAULT nextval('word_id_seq'),
    text character varying NOT NULL,
    weight decimal NOT NULL
);

CREATE TABLE association (
    id integer PRIMARY KEY DEFAULT nextval('association_id_seq'),
    word_id integer NOT NULL references word(id),
    sentence_id integer NOT NULL references sentence(id),
    weight decimal NOT NULL
);

CREATE TABLE chat (
    id integer PRIMARY KEY DEFAULT nextval('chat_id_seq'),
    interlocutor character varying NOT NULL,
    sentence_id integer NOT NULL references sentence(id)
);
