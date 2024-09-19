ollama run llama3.1

Docker
docker run -d -v ollama:/root/.ollama -p 11434:11434 --name ollama ollama/ollama

	docker exec -it ollama ollama run llama3


**Milvus:**
https://milvus.io/docs/install_standalone-docker.md

	curl -sfL https://raw.githubusercontent.com/milvus-io/milvus/master/scripts/standalone_embed.sh -o standalone_embed.sh

	bash standalone_embed.sh start

		
	https://docs.spring.io/spring-ai/reference/api/vectordbs/milvus.html
