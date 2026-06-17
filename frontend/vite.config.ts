import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vitest/config';
import path from 'path';

export default defineConfig({
	plugins: [sveltekit()],
	server: {
		proxy: {
			'/api': {
				target: 'http://localhost:8080',
				changeOrigin: true
			}
		}
	},
	resolve: {
		conditions: ['browser']
	},
	test: {
		include: ['src/**/*.{test,spec}.{js,ts}'],
		globals: true,
		environment: 'jsdom',
		setupFiles: ['src/tests/setup.ts'],
		alias: {
			$lib: path.resolve('./src/lib'),
			'$app/navigation': path.resolve('./src/tests/mocks/app-navigation.ts'),
			'$app/stores': path.resolve('./src/tests/mocks/app-stores.ts')
		}
	}
});
