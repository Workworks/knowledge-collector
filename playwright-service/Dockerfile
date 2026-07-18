FROM mcr.microsoft.com/playwright:v1.61.1-noble
WORKDIR /service
COPY package.json server.mjs ./
RUN npm install --omit=dev
EXPOSE 3003
HEALTHCHECK --interval=15s --timeout=5s --retries=5 CMD curl -fsS http://127.0.0.1:3003/health || exit 1
CMD ["node","server.mjs"]
