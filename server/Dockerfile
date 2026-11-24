# Serveur de jeu (server.py, UDP) : voir docker-compose.yml
FROM python:3.12-slim

ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1 PYTHONIOENCODING=utf-8
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
RUN useradd --system --no-create-home petsoccer
USER petsoccer
EXPOSE 7777/udp
CMD ["python", "-u", "server.py", "--port", "7777"]
