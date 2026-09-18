FROM node:22-alpine

WORKDIR /app
COPY --chown=node:node package.json generator.js messages.example.json ./

USER node
ENTRYPOINT ["node", "generator.js"]