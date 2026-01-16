--
-- PostgreSQL database dump
--

\restrict FSvEoG58u6CV5c6QIwMqymA0XAifxdqCaNsHY99lGHaVBq0N2RUZxHmbd9h4c7k

-- Dumped from database version 16.11 (Debian 16.11-1.pgdg13+1)
-- Dumped by pg_dump version 16.11 (Debian 16.11-1.pgdg13+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: user; Type: TABLE; Schema: public; Owner: MoSa
--

CREATE TABLE public."user" (
    id bigint NOT NULL,
    first_name character varying(255) NOT NULL,
    last_name character varying(255) NOT NULL,
    picture bytea,
    email character varying(255) NOT NULL,
    password character varying(255) NOT NULL,
    address character varying(255),
    city character varying(255),
    country character varying(255),
    phone_number character varying(255),
    description text,
    created_at date NOT NULL,
    updated_at date
);


ALTER TABLE public."user" OWNER TO "MoSa";

--
-- Name: user_id_seq; Type: SEQUENCE; Schema: public; Owner: MoSa
--

CREATE SEQUENCE public.user_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.user_id_seq OWNER TO "MoSa";

--
-- Name: user_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: MoSa
--

ALTER SEQUENCE public.user_id_seq OWNED BY public."user".id;


--
-- Name: user id; Type: DEFAULT; Schema: public; Owner: MoSa
--

ALTER TABLE ONLY public."user" ALTER COLUMN id SET DEFAULT nextval('public.user_id_seq'::regclass);


--
-- Data for Name: user; Type: TABLE DATA; Schema: public; Owner: MoSa
--

COPY public."user" (id, first_name, last_name, picture, email, password, address, city, country, phone_number, description, created_at, updated_at) FROM stdin;
\.


--
-- Name: user_id_seq; Type: SEQUENCE SET; Schema: public; Owner: MoSa
--

SELECT pg_catalog.setval('public.user_id_seq', 1, false);


--
-- Name: user user_email_key; Type: CONSTRAINT; Schema: public; Owner: MoSa
--

ALTER TABLE ONLY public."user"
    ADD CONSTRAINT user_email_key UNIQUE (email);


--
-- Name: user user_pkey; Type: CONSTRAINT; Schema: public; Owner: MoSa
--

ALTER TABLE ONLY public."user"
    ADD CONSTRAINT user_pkey PRIMARY KEY (id);


--
-- PostgreSQL database dump complete
--

\unrestrict FSvEoG58u6CV5c6QIwMqymA0XAifxdqCaNsHY99lGHaVBq0N2RUZxHmbd9h4c7k

